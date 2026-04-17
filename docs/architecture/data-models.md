# 数据模型

WenJuanPro 的核心数据模型均为**内存不可变数据**（`data class`），由解析器从 TXT 生成，由格式化器序列化写回 TXT。所有模型均放在 `:app` 模块的 `domain.model` 包下，不依赖 Android Framework，可直接在 JVM 单测中验证。

## Config

**用途:** 单个测评集的不可变快照，由 `/sdcard/WenJuanPro/config/{name}.txt` 解析而来。

**核心设计约束（v1.1 锁定）:** 一份 config.txt 支持配置**不限数量**的题目（`[Q1]`、`[Q2]`、`[Q3]`…），且**题型可任意混排**——单选（`single`）、多选（`multi`）、记忆闪烁点击题（`memory`）可以出现在同一份 config 中，以研究员指定的顺序依次呈现。`questions: List<Question>` 中的每个元素是 `Question` sealed interface 的三种具体实现之一，由解析器根据每道题的 `type` 字段动态派发。

**关键属性:**

- `configId`: String — 1-64 字符，仅 `[A-Za-z0-9_-]+`；缺失时回退为文件名（去 `.txt`）
- `title`: String — 展示给学生与研究员的标题
- `sourceFileName`: String — 源 TXT 文件名
- `questions`: List\<Question\> — 按 TXT 中 `[Q1] [Q2] …` 的出现顺序；**题型混排，不限数量**
- `parseWarnings`: List\<ConfigWarning\> — 如 BOM 存在等非致命警告

### Kotlin 数据类

```kotlin
data class Config(
    val configId: String,
    val title: String,
    val sourceFileName: String,
    val questions: List<Question>,
    val parseWarnings: List<ConfigWarning> = emptyList(),
) {
    val totalDurationMs: Long
        get() = questions.sumOf { it.totalDurationMs }
}

data class ConfigWarning(val line: Int, val message: String)
```

### 关系

- `Config` **聚合** `Question`（父子）
- `Config` **被** `Session` 引用（一对多：同 config 可被多位学生作答，各产生独立 Session）

## Question（sealed class）

**用途:** 题目多态，覆盖单选 / 多选 / 记忆三种题型；同屏（`all_in_one`）与分阶段（`staged`）呈现模式体现在倒计时字段上。

### Kotlin 数据类

```kotlin
sealed interface Question {
    val qid: String              // "Q1", "Q2", ...
    val mode: PresentMode        // ALL_IN_ONE | STAGED
    val stemDurationMs: Long?    // 仅 STAGED 模式下非空
    val optionsDurationMs: Long  // 同屏模式下代表整题倒计时
    val totalDurationMs: Long
        get() = (stemDurationMs ?: 0L) + optionsDurationMs

    data class SingleChoice(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,
        val stem: StemContent,
        val options: List<OptionContent>,
        val correctIndex: Int,           // 1-based
        val scores: List<Int>,           // size == options.size
    ) : Question

    data class MultiChoice(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,
        val stem: StemContent,
        val options: List<OptionContent>,
        val correctIndices: Set<Int>,    // 1-based
        val scores: List<Int>,
    ) : Question

    data class Memory(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,  // 这里是"复现阶段"倒计时
        val dotsPositions: List<Int>,    // 恰好 10 个，0..63，互不重复
        val flashDurationMs: Long = 1000L,
        val flashIntervalMs: Long = 500L,
    ) : Question
}

enum class PresentMode { ALL_IN_ONE, STAGED }

sealed interface StemContent {
    data class Text(val text: String) : StemContent
    data class Image(val assetName: String) : StemContent
    data class Mixed(val text: String, val assetName: String) : StemContent
}

sealed interface OptionContent {
    data class Text(val text: String) : OptionContent
    data class Image(val assetName: String) : OptionContent
    data class Mixed(val text: String, val assetName: String) : OptionContent
}
```

### 关系

- `Question` 被 **渲染** 为对应 Composable 作答页（见「UI 层组件架构」）
- `Question` 的正确答案 / 得分**永不呈现给学生**，仅在 `ResultRecord` 中写入供研究员比对

## Session

**用途:** 一次测评从扫码开始到完成的运行时上下文；生命周期绑定 NavHost 的 Session 图；异常终止后由断点续答恢复。

### Kotlin 数据类

```kotlin
data class Session(
    val studentId: String,            // 扫码所得
    val deviceId: String,             // Settings.Secure.ANDROID_ID
    val config: Config,
    val sessionStart: LocalDateTime,  // 点击「开始答题」瞬时
    val resultFileName: String,       // {deviceId}_{studentId}_{configId}_{yyyyMMdd-HHmmss}.txt
    val cursor: Int,                  // 指向下一道待作答题的 index，断点续答时 > 0
    val completedQids: Set<String>,   // 已 done/not_answered/error 的 qid
)
```

### 关系

- `Session` **引用** `Config`（不可变快照）
- `Session` **拥有** 一个结果文件（一对一）

## ResultRecord

**用途:** 结果 TXT 的一条业务行（header 下方）；管道分隔；由 `ResultFormatter` 序列化，由 `ResultRepository.append()` 追加。

### Kotlin 数据类

```kotlin
data class ResultRecord(
    val qid: String,
    val type: QuestionType,         // SINGLE | MULTI | MEMORY
    val mode: PresentMode,
    val stemMs: Long?,              // all_in_one 模式为 null，序列化为 "-"
    val optionsMs: Long,
    val answer: String,             // 单选: "2"；多选: "1,3"；记忆: "3,7,22,12,..."; 未答: ""
    val correct: String,            // 同格式
    val score: Int,
    val status: ResultStatus,       // DONE | NOT_ANSWERED | PARTIAL | ERROR
)

enum class QuestionType { SINGLE, MULTI, MEMORY }
enum class ResultStatus { DONE, NOT_ANSWERED, PARTIAL, ERROR }
```

### 关系

- `ResultRecord` 与 `Question` 一一对应（通过 `qid` 关联）

---
