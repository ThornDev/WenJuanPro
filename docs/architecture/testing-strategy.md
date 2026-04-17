# 测试策略

## 测试金字塔

```text
             UI / E2E（Compose Test）
                1-2 条扫码→作答→落盘→续答主路径
           /                                 \
      集成（Robolectric）
        FileSystem 真实读写 / SSAID / 权限分支
     /                                           \
 JVM 单测（JUnit4 + MockK + coroutines-test）        【主战场】
  ConfigParser / ResultFormatter / ResultParser
  QuestionFsm / ResumeFsm
  ScoreSingleChoice / ScoreMulti / ScoreMemory
  DetectResumableSessionUseCase
  ResultRepositoryImpl（假 FileSystem 验证原子追加）
```

**覆盖率目标:**

- 解析器 + 状态机 + 评分器 **≥ 80% 行覆盖**
- Repository 原子追加路径 **100% 分支覆盖**（成功 / write 失败 / fsync 失败 / 目录缺失）
- UI 层不强制覆盖率

## 测试组织

### 前端测试（Compose UI）

```text
app/src/androidTest/java/ai/wenjuanpro/app/ui/
├── ScanToAppendE2ETest.kt          # 扫码（模拟）→ 欢迎 → 单选 → 落盘 → 验证文件内容
├── ResumeSessionE2ETest.kt         # 预置 result 文件 → 扫码 → 跳到第三题
└── MemoryQuestionRenderTest.kt     # 仅验证点阵渲染与命中判定；时序不自动化
```

### 后端测试（Parser / Repository / UseCase — 均为 JVM 测试）

```text
app/src/test/java/ai/wenjuanpro/app/
├── data/parser/
│   ├── ConfigParserTest.kt         # 合法 config / 缺 title / 非法 type / 非法 durationMs / BOM / asset 缺失
│   ├── ResultFormatterTest.kt      # 单选 / 多选 / 记忆 / not_answered / staged stemMs=-
│   └── ResultParserTest.kt         # 完整文件 → completedQids / 半行容错（忽略非法尾行）
├── domain/fsm/
│   ├── QuestionFsmTest.kt          # 同屏 / 分阶段 / 题干超时自动切换 / 异常进入 error
│   └── ResumeFsmTest.kt            # 新会话 / 有进度 / 全部完成 / config 漂移
├── domain/usecase/
│   ├── ScoreSingleChoiceUseCaseTest.kt    # 命中 / 未命中 / 超时
│   ├── ScoreMultiChoiceUseCaseTest.kt     # 按选项独立得分之和
│   ├── ScoreMemoryUseCaseTest.kt          # 前缀匹配 / 全对 / 部分前缀 / 空答
│   └── DetectResumableSessionUseCaseTest.kt
└── data/result/
    └── ResultRepositoryImplTest.kt        # 假 FileSystem：append / fsync 失败分支 / 目录创建
```

### E2E 测试（覆盖于 Android 端 UI 测试）

见上文 `androidTest/`。

## 测试示例

### 前端组件测试

```kotlin
@RunWith(AndroidJUnit4::class)
class CountdownBarTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun warningColor_whenProgressBelow5sThreshold() {
        rule.setContent {
            CountdownBar(progress = 0.10f, isWarning = true)
        }
        rule.onNodeWithTag("countdown-bar")
            .assertIsDisplayed()
        // 颜色检验用 semantics 或 Screenshot test（可选）
    }
}
```

### 后端 API 测试（= Repository 原子追加测试）

```kotlin
class ResultRepositoryImplTest {
    private val fakeFs = FakeFileSystem()
    private val repo = ResultRepositoryImpl(fakeFs, ResultFormatter(), ResultParser(), Dispatchers.Unconfined)

    @Test
    fun `append writes complete line with fsync`() = runTest {
        repo.openSession(sampleSession)
        repo.append(sampleDoneRecord).getOrThrow()
        assertThat(fakeFs.readText(sampleSession.resultFileName))
            .contains("Q1|single|all_in_one|-|24530|2|1|0|done\n")
        assertThat(fakeFs.fsyncCalls).isEqualTo(1)
    }

    @Test
    fun `append propagates failure when fsync fails`() = runTest {
        fakeFs.failNextFsync = true
        repo.openSession(sampleSession)
        val result = repo.append(sampleDoneRecord)
        assertThat(result.isFailure).isTrue()
    }
}
```

### E2E 测试

```kotlin
@HiltAndroidTest
class ScanToAppendE2ETest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun singleChoice_completeFlow_writesResult() {
        // 预置 config
        testFileSystem.writeText("/sdcard/WenJuanPro/config/t1.txt", sampleValidConfig)
        composeRule.onNodeWithText("t1").performClick()
        // 注入假的扫码结果
        scanAutomator.emit("S001")
        composeRule.onNodeWithText("开始答题").performClick()
        composeRule.onNodeWithText("选项 A").performClick()
        composeRule.onNodeWithText("提交").performClick()
        // 断言 result 文件内容
        val content = testFileSystem.readText("/sdcard/WenJuanPro/results/")
        assertThat(content).contains("Q1|single|all_in_one|")
    }
}
```

---
