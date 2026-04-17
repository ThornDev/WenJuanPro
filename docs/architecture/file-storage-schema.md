# 文件存储 Schema

此节替代传统「API 契约 + 数据库 Schema」，是 Dev / QA / 研究员三方共识的硬性契约。任何对下述格式的修改**必须**经 Architect 发起 TCP（Technical Change Proposal）。

## 目录布局

```text
/sdcard/WenJuanPro/
├── config/          # 研究员预置的 TXT 题库；App 只读
│   └── {anyName}.txt
├── assets/          # 题干 / 选项引用的图片（PNG/JPG）；App 只读
│   └── {fileName}.png
├── results/         # App 追加写入；研究员回收后可读、清理
│   └── {deviceId}_{studentId}_{configId}_{yyyyMMdd-HHmmss}.txt
└── .diag/           # 诊断日志（可选）；不含答题明文
    └── app.log
```

## Config 文件 Schema

**编码:** UTF-8（无 BOM；BOM 存在时解析器告警但仍继续）  
**行尾:** `\n` 或 `\r\n` 均兼容  
**分节:** 以 `[Qn]` 行（`n` 为 1-based 编号）标记题目起始；`[Qn]` 之前的 `# key: value` 行为 Header  
**题目数量与题型混排:** 不限题目数量；每道题通过 `type` 字段独立声明题型（`single` / `multi` / `memory`），同一份 config 中可任意穿插。题目按 `[Q1]` → `[Q2]` → … 的出现顺序呈现给学生

**语法（BNF 风格）:**

```text
config      ::= header '\n' question+
header      ::= ( comment_line | kv_header_line )+
comment_line ::= '#' text '\n'
kv_header_line ::= '#' WS key ':' WS value '\n'
  # 必填 key: configId, title

question    ::= '[Q' digit+ ']' '\n' kv_line+
kv_line     ::= key ':' WS value '\n'

# 所有题目通用 key: type, mode
# all_in_one 模式 key: durationMs
# staged     模式 key: stemDurationMs, optionsDurationMs
# single / multi 题型 key: stem, options, correct, score
# memory 题型 key: dotsPositions(, flashDurationMs, flashIntervalMs — 可选，缺失用默认 1000/500)
# options 值以竖线 | 分隔；支持 "img:filename.png" 前缀表示图片；支持 "img:file.png|文字部分" 混合
```

**示例 — 单选（同屏模式）:**

```text
# configId: cog-mem-2026q3
# title: 认知记忆测评 v1

[Q1]
type: single
mode: all_in_one
durationMs: 30000
stem: 你今天感觉如何？
options: 很好|一般|较差|很差
correct: 1
score: 2|1|0|0
```

**示例 — 多选（分阶段模式）:**

```text
[Q2]
type: multi
mode: staged
stemDurationMs: 10000
optionsDurationMs: 20000
stem: 下面哪些是水果？
options: 苹果|胡萝卜|香蕉|菠菜
correct: 1,3
score: 1|0|1|0
```

**示例 — 记忆题:**

```text
[Q3]
type: memory
mode: all_in_one
optionsDurationMs: 20000
dotsPositions: 3,7,12,19,22,30,37,44,51,58
# 可选（缺失则硬编码默认）:
# flashDurationMs: 1000
# flashIntervalMs: 500
```

**Schema 校验规则:** 详见 PRD Story 1.3 `data_validation` 与 `error_handling` 章节；所有错误必须包含行号、字段、原因三要素。

## Result 文件 Schema

**文件名:** `{deviceId}_{studentId}_{configId}_{yyyyMMdd-HHmmss}.txt`  
**编码:** UTF-8（无 BOM）  
**结构:** Header（key:value 行）→ `---` 分隔行 → 业务行（每题一行，管道分隔）

**Header 示例:**

```text
deviceId: abc123def456
studentId: S001
configId: cog-mem-2026q3
sessionStart: 20260417-103045
appVersion: 0.1.0
---
```

**业务行格式（每题一行，9 字段，`|` 分隔）:**

```text
qid | type | mode | stemMs | optionsMs | answer | correct | score | status
```

- `type`: `single` / `multi` / `memory`
- `mode`: `all_in_one` / `staged`
- `stemMs`: `staged` 模式下为毫秒数；`all_in_one` 模式写 `-`
- `answer` / `correct`:
  - 单选: `"2"` / `"1"`
  - 多选: `"1,3"` / `"1,3"`（逗号分隔，升序）
  - 记忆: `"3,7,22,..."` / `"7,22,3,..."`（学生复现顺序 vs 本次闪烁顺序）
  - 未答: `""`
- `status`: `done` / `not_answered` / `partial` / `error`
  - **注**: 依 PRD BR-2.5 规则，`staged` 模式只在整题结束（选项阶段提交/超时/异常）时写入一行；**运行中的 `partial` 不会落到文件**，以简化续答语义

**业务行示例:**

```text
Q1|single|all_in_one|-|24530|2|1|0|done
Q2|multi|staged|10000|8420|1,3|1,2|1|done
Q3|memory|all_in_one|-|18200|3,7,22,12,19,30,37,44,51,58|7,22,3,12,19,30,37,44,51,58|8|done
Q4|single|all_in_one|-|30000||2|0|not_answered
```

## 断点续答探测规则

- **同设备约束（v1.1 锁定）:** 仅匹配 `{当前deviceId}_{studentId}_{configId}_` 前缀的文件；不同 `deviceId` 的文件**视为不存在**，即**跨设备不续答**（语义最清晰，避免学生换设备后出现"别人的进度"干扰）
- 若匹配多个文件（理论上不应出现——同设备同学号同 config 已有完成文件时视为全部完成）——取 `sessionStart` 最晚的一份
- 已完成 qid = 文件中所有 `status ∈ {done, not_answered}` 的行（`error` 与缺失视为未完成）
- config 漂移检测：若文件中出现的 qid 不是 config 的前缀子集（如 qid 重命名、题目数量变更），禁用「继续上次答题」按钮

---
