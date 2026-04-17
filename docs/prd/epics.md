# Epics

⚠️ 以下 Epic 定义是后续 SM 渲染 Story 的唯一权威来源；所有业务规则、验证、错误处理须由 PM 在此完成，SM 不再追加分析。

## Epic 1: 项目基础与配置加载

**Epic 概述:** 建立 Android 工程骨架（Compose + MVI + Hilt + 协程 + Material 3），完成 `MANAGE_EXTERNAL_STORAGE` 授权引导，实现 TXT config 解析器与 schema 校验，并交付测评集选择页作为首屏。研究员安装 APK 后即可验证自己的 TXT 配置是否能被正确加载。

**目标仓库:** monolith

```yaml
epic_id: 1
title: "项目基础与配置加载"
description: |
  建立 Android 工程骨架与文件 IO 基础设施，交付测评集选择页作为首屏可见功能。
  完成本 Epic 后，研究员可独立验证 /sdcard/WenJuanPro/config/ 下 TXT 配置是否正确，
  并对损坏文件获得明确的行号级错误提示。

stories:
  - id: "1.1"
    title: "工程骨架与导航框架"
    repository_type: monolith
    estimated_complexity: low
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "Android 工程可成功构建并启动到首屏"
        scenario:
          given: "新建 Android Studio 工程，最低 API 26、目标 API 34"
          when: "执行 ./gradlew assembleDebug 并安装到设备"
          then:
            - "工程使用 Kotlin 1.9+、Compose BOM、Material 3"
            - "App 启动后呈现一个空白 Composable 占位首屏（标题: WenJuanPro）"
            - "已配置 Hilt + Coroutines + Timber + ktlint"
            - "屏幕方向锁定为 portrait"
        business_rules:
          - id: "BR-1.1"
            rule: "App 仅依赖白名单内的库；禁止出现 OkHttp / Retrofit / Firebase / 任何统计 SDK"
          - id: "BR-1.2"
            rule: "package name 为 ai.wenjuanpro.app；applicationId 同名"
        error_handling:
          - scenario: "构建依赖冲突或 SDK 版本不匹配"
            code: "BUILD_ERROR"
            message: "构建失败：{原因}"
            action: "停止构建，控制台输出修复建议"
        examples:
          - input: "./gradlew assembleDebug"
            expected: "BUILD SUCCESSFUL，APK 产物可在真机/模拟器安装并启动"
    provides_apis: []
    consumes_apis: []
    dependencies: []
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "1.2"
    title: "MANAGE_EXTERNAL_STORAGE 授权引导"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "未授权时引导用户跳转系统设置"
        scenario:
          given: "App 首次启动，MANAGE_EXTERNAL_STORAGE 未授权"
          when: "用户进入 App 首屏"
          then:
            - "App 自动检测授权状态（Environment.isExternalStorageManager()）"
            - "未授权时跳转到授权引导页（不能进入测评集选择页）"
            - "授权引导页展示『需要全部文件访问权限以读取题库与写入结果』说明文案"
            - "提供『前往系统设置授权』按钮，点击后跳转 ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION"
            - "授权引导页提供『已完成授权，重新检测』按钮"
        business_rules:
          - id: "BR-1.1"
            rule: "在 Android 11+ (API 30+) 使用 MANAGE_EXTERNAL_STORAGE；Android 10 及以下使用传统 READ/WRITE_EXTERNAL_STORAGE 兼容路径"
          - id: "BR-1.2"
            rule: "授权检测发生在每次 onResume，确保用户从系统设置返回后立即生效"
          - id: "BR-1.3"
            rule: "授权状态变更不需要重启 App"
        error_handling:
          - scenario: "用户拒绝授权或返回时仍未授权"
            code: "PERMISSION_DENIED"
            message: "未获取存储权限，无法读取题库。请前往系统设置开启『所有文件访问权限』"
            action: "保持在授权引导页，按钮文案变为『再次前往系统设置』"
          - scenario: "设备 ROM 不支持该 Intent"
            code: "INTENT_UNAVAILABLE"
            message: "无法自动跳转到授权页面，请手动前往：设置 → 应用 → WenJuanPro → 权限 → 全部文件访问"
            action: "展示纯文本指引，提供复制路径按钮"
        interaction:
          - trigger: "首次启动且未授权"
            behavior: "首屏直接显示授权引导页，无淡入动画"
          - trigger: "点击『前往系统设置授权』"
            behavior: "跳转外部系统设置；返回 App 后 onResume 自动重新检测"
          - trigger: "授权成功后"
            behavior: "自动导航到测评集选择页（Story 1.4），无需手动确认"
        examples:
          - input: "首次启动，无任何权限"
            expected: "授权引导页 + 跳转设置按钮"
          - input: "授权后从系统设置返回"
            expected: "自动检测到已授权，导航到测评集选择页"
    provides_apis: []
    consumes_apis: []
    dependencies: ["1.1"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "1.3"
    title: "TXT config 解析器与 schema 校验"
    repository_type: monolith
    estimated_complexity: high
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "解析合法 config TXT 文件"
        scenario:
          given: "/sdcard/WenJuanPro/config/ 下存在一份合法的 cog-mem-2026q3.txt"
          when: "App 启动后扫描该目录并调用 ConfigRepository.loadAll()"
          then:
            - "解析器返回 Config 对象，包含 configId、title、题目数组"
            - "每道题包含 qid、type (single/multi/memory)、mode (all_in_one/staged)、durationMs / stemDurationMs+optionsDurationMs、题干、选项、正确答案、得分"
            - "解析过程在 IO Dispatcher 执行，不阻塞主线程"
        business_rules:
          - id: "BR-1.1"
            rule: "config 文件 header 必须包含 configId 与 title 两个字段；缺失任一字段则该 config 标记为损坏"
          - id: "BR-1.2"
            rule: "configId 缺失时回退使用文件名（去 .txt 扩展）作为 configId"
          - id: "BR-1.3"
            rule: "题目以 [Q1] [Q2] ... 形式分节，节内 key:value 行；section 顺序即题目顺序"
          - id: "BR-1.4"
            rule: "选项分隔符为竖线 |；选项可包含图片引用 `img:文件名.png`（文件须存在于 /sdcard/WenJuanPro/assets/）"
          - id: "BR-1.5"
            rule: "memory 题型固定 64 格点阵，10 个蓝点位置由 config 中的 dotsPositions 字段指定（10 个 0-63 索引）"
          - id: "BR-1.6"
            rule: "文件编码必须为 UTF-8（无 BOM）；BOM 存在时报警告但仍可解析"
        data_validation:
          - field: "configId"
            type: "string"
            required: true
            rules: "1-64 字符，仅限字母、数字、连字符、下划线"
            error_message: "configId 格式非法：只允许字母、数字、-、_"
          - field: "题目类型 type"
            type: "string"
            required: true
            rules: "枚举: single | multi | memory"
            error_message: "未知题型：{值}（合法值: single/multi/memory）"
          - field: "呈现模式 mode"
            type: "string"
            required: true
            rules: "枚举: all_in_one | staged"
            error_message: "未知呈现模式：{值}（合法值: all_in_one/staged）"
          - field: "倒计时 durationMs / stemDurationMs / optionsDurationMs"
            type: "number"
            required: true
            rules: "正整数，1000-600000（即 1 秒至 10 分钟）"
            error_message: "倒计时时长非法：必须为 1000-600000 毫秒之间的正整数"
          - field: "memory 题 dotsPositions"
            type: "array"
            required: true
            rules: "恰好 10 个互不重复的 0-63 整数"
            error_message: "记忆题点位非法：必须为 10 个互不重复的 0-63 索引"
        error_handling:
          - scenario: "config 文件 header 缺失必填字段"
            code: "CONFIG_HEADER_MISSING"
            message: "{文件名}: 第 {行号} 行缺少必填字段 {字段名}"
            action: "该 config 标记为 invalid，不阻塞其他合法 config 的加载"
          - scenario: "题目字段不合法（类型、模式、倒计时越界）"
            code: "CONFIG_FIELD_INVALID"
            message: "{文件名} [{Qid}] 第 {行号} 行: {具体原因}"
            action: "该 config 标记为 invalid，错误详情可在测评集选择页查看"
          - scenario: "选项中引用的图片文件不存在"
            code: "ASSET_NOT_FOUND"
            message: "{文件名} [{Qid}]: 选项引用的图片 {路径} 不存在"
            action: "该 config 标记为 invalid"
          - scenario: "文件编码非 UTF-8"
            code: "ENCODING_INVALID"
            message: "{文件名}: 文件编码非 UTF-8，无法解析"
            action: "该 config 标记为 invalid"
        examples:
          - input: |
              # configId: cog-mem-2026q3
              # title: 认知记忆测评 v1
              [Q1]
              type: single
              mode: all_in_one
              durationMs: 30000
              stem: 你今天感觉如何？
              options: 很好|一般|较差|很差
              correct: 1
              score: 0|0|0|0
            expected: "Config(configId=cog-mem-2026q3, title=认知记忆测评 v1, questions=[Q1: SingleChoice(...)])"
          - input: "header 缺失 title"
            expected: "Invalid 标记 + 错误信息: 'cog-mem-2026q3.txt: 第 2 行缺少必填字段 title'"
    provides_apis: []
    consumes_apis: []
    dependencies: ["1.1"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "1.4"
    title: "测评集选择页（首屏）"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "展示所有 config 文件并区分有效/损坏"
        scenario:
          given: "已授权 + /sdcard/WenJuanPro/config/ 下有 N 个 config 文件（部分损坏）"
          when: "App 进入测评集选择页"
          then:
            - "页面顶部显示『请选择测评集』标题与刷新按钮"
            - "以卡片列表呈现所有 config，每张卡片包含 configId、title、题数、状态徽标（有效/损坏）"
            - "有效卡片可点击；损坏卡片置灰且不可点击，但可点击『查看错误详情』查看具体错误信息"
            - "无任何 config 时展示空态：『请将 config TXT 文件放入 /sdcard/WenJuanPro/config/ 后点击刷新』"
        business_rules:
          - id: "BR-1.1"
            rule: "刷新按钮触发 ConfigRepository.loadAll() 重新扫描目录"
            "扫描必须在 IO 线程，UI 显示加载指示器（最长 5 秒，超时显示失败提示）"
          - id: "BR-1.2"
            rule: "点击有效 config 卡片后，将该 configId 存入会话状态并导航到扫码登录页（Story 2.1）"
          - id: "BR-1.3"
            rule: "config 列表按 configId 字典序排序"
        data_validation: []
        error_handling:
          - scenario: "config 目录不存在"
            code: "CONFIG_DIR_MISSING"
            message: "未找到题库目录 /sdcard/WenJuanPro/config/，已自动创建"
            action: "自动创建空目录，展示空态文案"
          - scenario: "扫描超时（5s）"
            code: "SCAN_TIMEOUT"
            message: "扫描题库超时，请检查目录是否存在大量文件"
            action: "停止扫描，展示重试按钮"
        interaction:
          - trigger: "进入页面"
            behavior: "自动触发一次 loadAll()，展示加载指示器"
          - trigger: "点击刷新按钮"
            behavior: "重新扫描；按钮变灰直到完成"
          - trigger: "点击损坏 config 的『查看错误详情』"
            behavior: "弹出底部 sheet 展示完整错误列表（行号 + 字段 + 原因）"
        examples:
          - input: "目录下有 cog-mem-2026q3.txt（合法）+ broken.txt（缺 title）"
            expected: "两张卡片：cog-mem-2026q3 标记『有效，5 题』可点；broken 标记『损坏』置灰但可查看详情"
          - input: "目录为空"
            expected: "空态文案 + 刷新按钮"
    provides_apis: []
    consumes_apis: []
    dependencies: ["1.2", "1.3"]
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 2: 扫码登录与基础题型作答

**Epic 概述:** 实现扫码登录 → 欢迎确认 → 单选/多选题作答（含同屏与分阶段模式倒计时与进度条）→ 结果文件实时落盘的端到端主流程。完成本 Epic 后，App 已能跑通一份不含记忆题的"普通问卷"完整测评。

**目标仓库:** monolith

```yaml
epic_id: 2
title: "扫码登录与基础题型作答"
description: |
  实现 QR 扫码 → 欢迎页 → 单选/多选题（同屏 + 分阶段模式）→ 结果实时落盘的完整主路径。
  完成本 Epic 后即可承载一份不含记忆题的离线问卷测评。

stories:
  - id: "2.1"
    title: "扫码登录与学号识别"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "通过摄像头扫描二维码并将内容作为学号"
        scenario:
          given: "用户在测评集选择页选定一个有效 config 后进入扫码页"
          when: "用户将二维码对准取景框"
          then:
            - "App 调用 CameraX 预览 + ZXing Android Embedded 解码二维码内容"
            - "识别成功后将二维码内容作为学号（studentId）存入会话状态"
            - "自动导航到欢迎确认页（Story 2.2）"
            - "全程不提供任何键盘手输入口"
        business_rules:
          - id: "BR-1.1"
            rule: "学号格式约束：1-64 字符，仅限字母、数字、连字符、下划线；不符合则提示并继续扫码"
          - id: "BR-1.2"
            rule: "扫码成功后摄像头立即停止，避免重复识别"
          - id: "BR-1.3"
            rule: "需 CAMERA 运行时权限；首次进入时申请，拒绝则展示无摄像头提示并提供重新申请按钮"
          - id: "BR-1.4"
            rule: "不支持手动输入学号（防止数据污染）"
        data_validation:
          - field: "二维码内容（学号）"
            type: "string"
            required: true
            rules: "1-64 字符，正则 ^[A-Za-z0-9_-]+$"
            error_message: "学号格式非法：仅允许字母、数字、-、_"
        error_handling:
          - scenario: "学号格式非法"
            code: "STUDENT_ID_INVALID"
            message: "学号格式非法：{扫到的内容}"
            action: "保持扫码页打开，2 秒 toast 后继续等待新二维码"
          - scenario: "CAMERA 权限被拒"
            code: "CAMERA_DENIED"
            message: "未获取摄像头权限，无法扫码登录"
            action: "展示『重新申请权限』按钮 + 『前往系统设置』备选按钮"
          - scenario: "设备无后置摄像头"
            code: "CAMERA_UNAVAILABLE"
            message: "未检测到摄像头，无法扫码登录"
            action: "展示错误页，提供返回测评集选择页按钮"
        interaction:
          - trigger: "进入扫码页"
            behavior: "全屏摄像头预览 + 半透明取景框遮罩 + 提示文案『请将二维码对准取景框』"
          - trigger: "识别成功"
            behavior: "震动反馈一次（50ms），摄像头停止，立即跳转下一页"
          - trigger: "识别到非法学号"
            behavior: "底部 toast 显示错误，2 秒后自动消失，摄像头继续工作"
        examples:
          - input: "扫码内容: S001"
            expected: "学号 S001 入会话，跳转欢迎确认页"
          - input: "扫码内容: 含空格的字符串"
            expected: "底部 toast '学号格式非法'，继续扫码"
    provides_apis: []
    consumes_apis: []
    dependencies: ["1.4"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.2"
    title: "欢迎确认页"
    repository_type: monolith
    estimated_complexity: low
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "展示学号与测评信息并等待开始确认"
        scenario:
          given: "扫码成功后跳转到欢迎确认页"
          when: "页面渲染完成"
          then:
            - "顶部展示『欢迎，{学号}』"
            - "中部展示选定 config 的 title、题目总数、预计用时（所有题倒计时之和）"
            - "底部显示『开始答题』按钮"
            - "学生点击『开始答题』后记录测评开始时间戳，跳转到第一题"
        business_rules:
          - id: "BR-1.1"
            rule: "测评开始时间戳取点击『开始答题』的瞬时时间，格式 yyyyMMdd-HHmmss，用于结果文件命名"
          - id: "BR-1.2"
            rule: "如果该学号在该 configId 下已有未完成的结果文件存在，跳转到断点续答提示页（Story 4.1），而非首题"
          - id: "BR-1.3"
            rule: "预计用时按所有题（含 staged 模式两阶段）倒计时总和计算，向上取整到分钟"
        error_handling:
          - scenario: "无可读取的 SSAID"
            code: "SSAID_UNAVAILABLE"
            message: "无法读取设备 ID，请联系研究员"
            action: "阻塞，不允许进入作答；展示重试按钮"
        interaction:
          - trigger: "页面进入"
            behavior: "无入场动画"
          - trigger: "点击『开始答题』"
            behavior: "按钮立即禁用，500ms 内完成跳转，避免重复点击"
        examples:
          - input: "学号 S001 + 5 题 config（总时长 5 分钟）"
            expected: "页面显示『欢迎，S001 / 认知记忆测评 v1 / 5 题 / 预计 5 分钟』"
    provides_apis: []
    consumes_apis: []
    dependencies: ["2.1"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.3"
    title: "单选题渲染与作答（同屏 + 分阶段模式）"
    repository_type: monolith
    estimated_complexity: high
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "同屏模式：题干 + 选项同屏，单倒计时"
        scenario:
          given: "当前题为 type=single, mode=all_in_one, durationMs=30000"
          when: "页面进入"
          then:
            - "顶部进度条从 100% 起始按 30 秒倒计时线性递减"
            - "题干区显示文字（或图片或混合）"
            - "选项区显示 N 个选项（每行 2 或 3 个，由选项总数自动决定）"
            - "学生点击某选项后该选项呈选中态，其他取消"
            - "学生点击『提交』后立即记录答案、得分并写入结果文件"
            - "倒计时归零仍未提交则记『未作答』并写入结果文件，自动进入下一题"
        business_rules:
          - id: "BR-1.1"
            rule: "选项渲染规则: 总数 ≤ 4 → 每行 2 个；5-9 → 每行 3 个；> 9 → 每行 3 个并垂直滚动"
          - id: "BR-1.2"
            rule: "单选题选中是排他的；切换选项不重置倒计时"
          - id: "BR-1.3"
            rule: "得分 = 该选项在 score 数组中的对应值"
          - id: "BR-1.4"
            rule: "倒计时进度条颜色保持 Primary 蓝；剩余 ≤ 5 秒变橙；不弹任何提示"
        data_validation:
          - field: "学生答案 answer"
            type: "number"
            required: false
            rules: "1-N 之间的整数（N 为选项数），未作答时为空字符串"
            error_message: "（内部错误，不暴露给用户）"
        error_handling:
          - scenario: "结果写入失败"
            code: "RESULT_WRITE_FAILED"
            message: "数据保存失败，请联系研究员"
            action: "阻塞下一题跳转，展示重试按钮（再次写入），重试 3 次仍失败则展示终止页"
        interaction:
          - trigger: "页面进入"
            behavior: "倒计时立即开始；提交按钮置灰直到至少选中一个选项"
          - trigger: "选项点击"
            behavior: "选中态动画（背景色淡入 100ms）；提交按钮可用"
          - trigger: "倒计时 ≤ 5 秒"
            behavior: "进度条颜色变橙，无音效"
          - trigger: "提交或超时"
            behavior: "立即跳转下一题，无确认弹窗"
        examples:
          - input: "30s 内选中选项 2 后提交"
            expected: "结果记录: answer=2, score={score[1]}, optionsMs=实际用时, status=done"
          - input: "30s 倒计时归零未提交"
            expected: "结果记录: answer=, score=0, optionsMs=30000, status=not_answered"

      - id: AC2
        title: "分阶段模式：题干阶段独立倒计时，超时自动进入选项阶段"
        scenario:
          given: "当前题为 type=single, mode=staged, stemDurationMs=10000, optionsDurationMs=20000"
          when: "页面进入"
          then:
            - "首先显示题干阶段：顶部进度条按 10 秒倒计时；屏幕仅显示题干，无选项"
            - "题干阶段无任何作答按钮，学生点击屏幕无反应（但不报错）"
            - "10 秒倒计时归零自动进入选项阶段（这是正常流转，不是异常）"
            - "选项阶段显示选项 + 提交按钮 + 新的 20 秒倒计时进度条；题干已隐藏"
            - "选项阶段提交或超时后写入结果，自动进入下一题"
        business_rules:
          - id: "BR-2.1"
            rule: "题干阶段无任何用户作答动作；题干阶段超时是『正常流转』，不记为错误也不影响后续选项阶段"
          - id: "BR-2.2"
            rule: "题干阶段实际用时固定为 stemDurationMs（即倒计时全程）；记录到 stemMs 字段"
          - id: "BR-2.3"
            rule: "选项阶段实际用时为学生进入选项阶段到提交/超时的耗时；记录到 optionsMs 字段"
          - id: "BR-2.4"
            rule: "若学生在选项阶段未作答且超时，记 status=not_answered；得分为 0"
        error_handling:
          - scenario: "题干阶段任意异常（如系统中断 ANR）"
            code: "STAGE_TRANSITION_FAILED"
            message: "数据保存失败，请联系研究员"
            action: "整题作废，记录 status=error，写入结果后跳过到下一题"
        interaction:
          - trigger: "题干阶段点击"
            behavior: "无视觉反馈，无错误提示（学生可能误操作，静默忽略）"
          - trigger: "题干阶段倒计时归零"
            behavior: "题干区淡出 200ms，选项区淡入 200ms，新进度条从 100% 起始"
          - trigger: "选项阶段提交或超时"
            behavior: "立即写入结果并跳转下一题"
        examples:
          - input: "题干 10s + 选项 20s，学生在选项阶段第 8s 选中并提交"
            expected: "结果记录: stemMs=10000, optionsMs=8000, answer=X, score=Y, status=done"
          - input: "题干 10s + 选项 20s，学生选项阶段未作答超时"
            expected: "结果记录: stemMs=10000, optionsMs=20000, answer=, score=0, status=not_answered"
    provides_apis: []
    consumes_apis: []
    dependencies: ["2.2", "1.3"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.4"
    title: "多选题渲染与作答（同屏 + 分阶段模式）"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "多选题选项可勾选/取消，得分按选项独立计算"
        scenario:
          given: "当前题为 type=multi, mode=all_in_one, durationMs=30000"
          when: "页面进入"
          then:
            - "题干 + 选项同屏渲染，倒计时开始"
            - "学生可点击多个选项进行勾选；再次点击取消勾选"
            - "提交按钮置灰直到至少勾选一个选项"
            - "提交时记录已勾选选项序号集合，得分 = 各选中选项 score 之和"
            - "倒计时归零未提交则记 not_answered"
        business_rules:
          - id: "BR-1.1"
            rule: "多选题不要求学生选择正确数量；只要勾选 ≥ 1 项即可提交"
          - id: "BR-1.2"
            rule: "正确答案集合用于评分对照（写入结果文件 correct 字段），但不向学生展示对错"
          - id: "BR-1.3"
            rule: "得分 = sum(score[i] for i in selected)"
          - id: "BR-1.4"
            rule: "选项渲染规则与单选题一致（每行 2 或 3 个）"
          - id: "BR-1.5"
            rule: "分阶段模式与 Story 2.3 AC2 完全一致（题干阶段超时自动进入选项阶段）"
        data_validation:
          - field: "学生答案 answer"
            type: "string"
            required: false
            rules: "逗号分隔的选项序号集合，如 '1,3,4'；未作答时为空字符串"
            error_message: "（内部错误，不暴露给用户）"
        error_handling:
          - scenario: "结果写入失败"
            code: "RESULT_WRITE_FAILED"
            message: "数据保存失败，请联系研究员"
            action: "同 Story 2.3，重试 3 次后展示终止页"
        interaction:
          - trigger: "选项点击"
            behavior: "切换勾选态（背景色 + 复选标记），不互斥"
          - trigger: "勾选数从 0 变 1"
            behavior: "提交按钮启用"
          - trigger: "勾选数从 1 变 0"
            behavior: "提交按钮置灰"
        examples:
          - input: "选中选项 1 和 3，提交"
            expected: "answer='1,3', score=score[0]+score[2]"
          - input: "未勾选任何选项直至超时"
            expected: "answer='', status=not_answered, score=0"
    provides_apis: []
    consumes_apis: []
    dependencies: ["2.3"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.5"
    title: "结果文件实时落盘（命名 + 字段 + 原子追加）"
    repository_type: monolith
    estimated_complexity: high
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "结果文件按规范命名并在首次写入时创建 header"
        scenario:
          given: "学生在欢迎页点击『开始答题』触发 ResultRepository.startSession()"
          when: "首次写入第一题结果"
          then:
            - "若结果文件不存在则创建：/sdcard/WenJuanPro/results/{deviceId}_{studentId}_{configId}_{yyyyMMdd-HHmmss}.txt"
            - "deviceId = Settings.Secure.ANDROID_ID（无法读取时阻塞会话）"
            - "configId 取自 config 文件 header 或文件名"
            - "时间戳取『开始答题』点击的瞬时时间"
            - "首行写入 header 元数据：deviceId / studentId / configId / sessionStart / appVersion"
            - "header 后写入分隔行 ---"
            - "随后开始追加每题/每阶段记录行"
        business_rules:
          - id: "BR-1.1"
            rule: "header 格式（key:value 行）: deviceId、studentId、configId、sessionStart、appVersion"
          - id: "BR-1.2"
            rule: "追加每行的字段（管道分隔）: qid|type|mode|stemMs|optionsMs|answer|correct|score|status"
            "stemMs 仅 staged 模式有值；all_in_one 模式写 -"
          - id: "BR-1.3"
            rule: "status 枚举: done | not_answered | partial | error"
            "partial 仅在 staged 模式题干阶段已写入但选项阶段未完成时使用"
          - id: "BR-1.4"
            rule: "写入策略: 整行 append + flush + fsync；先组好整行再写，避免半行破坏"
          - id: "BR-1.5"
            rule: "在 staged 模式下，题干阶段结束（自动进入选项阶段）时不立即写一行；只有题目完整结束（选项阶段提交/超时/异常）才写一行；这避免续答时需要合并 partial 行"
            "（注：本规则使续答状态判定简化为按 qid 是否存在判定；半完成题不会落到结果文件中）"
          - id: "BR-1.6"
            rule: "若 BR-1.5 之外发生 App 崩溃导致选项阶段未写入，则下次续答时该 qid 视为未完成（整题重做，符合 Q4 决策）"
        data_validation:
          - field: "deviceId"
            type: "string"
            required: true
            rules: "Settings.Secure.ANDROID_ID 返回值；非空"
            error_message: "无法读取设备 ID"
        error_handling:
          - scenario: "目录不存在"
            code: "RESULT_DIR_MISSING"
            message: "结果目录不存在，已自动创建"
            action: "自动 mkdirs，继续写入"
          - scenario: "写入失败（磁盘满 / 权限丢失）"
            code: "RESULT_WRITE_FAILED"
            message: "数据保存失败，请联系研究员"
            action: "Story 2.3 AC1 已定义重试策略"
          - scenario: "fsync 失败"
            code: "FSYNC_FAILED"
            message: "数据落盘失败"
            action: "记录 .diag 日志，向上抛出 RESULT_WRITE_FAILED"
        examples:
          - input: "deviceId=abc123, studentId=S001, configId=cog-mem-2026q3, 开始时间 2026-04-17 10:30:45"
            expected: |
              文件名: abc123_S001_cog-mem-2026q3_20260417-103045.txt
              文件内容首段:
                deviceId: abc123
                studentId: S001
                configId: cog-mem-2026q3
                sessionStart: 20260417-103045
                appVersion: 0.1.0
                ---
          - input: "Q1 同屏模式答完: answer=2, correct=1, score=0, optionsMs=24530"
            expected: "追加行: Q1|single|all_in_one|-|24530|2|1|0|done"
          - input: "Q2 分阶段模式: stemMs=10000, optionsMs=8420, answer=1,3, correct=1,2, score=1, status=done"
            expected: "追加行: Q2|multi|staged|10000|8420|1,3|1,2|1|done"
    provides_apis: []
    consumes_apis: []
    dependencies: ["2.2"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.6"
    title: "完成页"
    repository_type: monolith
    estimated_complexity: low
    priority: P1
    acceptance_criteria:
      - id: AC1
        title: "所有题目完成后展示完成页"
        scenario:
          given: "最后一题完成（提交或超时）"
          when: "结果写入成功"
          then:
            - "导航到完成页，展示『测评已完成，请将设备交还研究员』"
            - "页面无『重新开始』按钮，无成绩反馈"
            - "30 秒后自动返回测评集选择页（避免学生忘记归还设备时占用界面）"
        business_rules:
          - id: "BR-1.1"
            rule: "完成页停留期间禁止返回作答页（系统返回键被消费）"
          - id: "BR-1.2"
            rule: "30 秒倒计时不可见，避免学生误以为是新一轮答题"
        error_handling:
          - scenario: "返回测评集选择页时 config 已被研究员替换"
            code: "CONFIG_CHANGED"
            message: "（无需提示，正常刷新即可）"
            action: "重新扫描 config 目录"
        interaction:
          - trigger: "进入完成页"
            behavior: "淡入 300ms"
          - trigger: "30 秒后"
            behavior: "自动返回测评集选择页，无动画"
        examples:
          - input: "Q5（最后一题）写入成功"
            expected: "完成页 + 30s 自动返回首页"
    provides_apis: []
    consumes_apis: []
    dependencies: ["2.5"]
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 3: 记忆闪烁点击题

**Epic 概述:** 实现 8×8 点阵渲染、Compose 原生动画驱动的闪烁序列、1.0× 半径命中判定、学生顺序复现的完整记忆题作答闭环；满足 ±50ms 视觉时序抖动目标。

**目标仓库:** monolith

```yaml
epic_id: 3
title: "记忆闪烁点击题"
description: |
  实现 WenJuanPro 的核心差异化范式：8×8 点阵 + 10 蓝点 + 随机顺序闪烁 + 顺序复现点击。
  视觉时序由 Compose 原生动画 API 驱动，时序抖动 ≤ ±50ms；命中判定使用 1.0× 圆点视觉半径，
  宁可误判『点偏』也不放宽容错（研究场景精确性优先）。

stories:
  - id: "3.1"
    title: "8×8 点阵渲染与初始 10 蓝点布局"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "渲染 8×8 点阵并固定 10 蓝点"
        scenario:
          given: "当前题为 type=memory，dotsPositions=[3, 7, 12, 19, 22, 30, 37, 44, 51, 58]（示例）"
          when: "页面进入"
          then:
            - "在屏幕中央渲染 8×8 = 64 个点位的网格"
            - "网格在屏幕中央居中，宽度占屏幕宽 90%（避免边缘被系统手势区遮挡）"
            - "10 个蓝点按 dotsPositions 在指定索引位置呈现（Primary 蓝 #1976D2 实心圆）"
            - "其他 54 个位置为浅灰色空圆（提示点位可见但无内容）"
            - "蓝点直径 = 网格单元宽度 × 0.7"
        business_rules:
          - id: "BR-1.1"
            rule: "网格索引: 0 表示左上，按行优先递增；63 表示右下"
          - id: "BR-1.2"
            rule: "网格在不同屏幕尺寸下保持等比例缩放，不变形"
          - id: "BR-1.3"
            rule: "顶部进度条与点阵之间留出至少 24dp 间距，避免视觉干扰"
        data_validation: []
        error_handling:
          - scenario: "dotsPositions 在 Story 1.3 已校验，运行时不应出现非法索引"
            code: "INTERNAL_DOT_INDEX_INVALID"
            message: "数据异常，请联系研究员"
            action: "终止当前题，写入 status=error，跳过到下一题"
        interaction:
          - trigger: "页面进入"
            behavior: "点阵无入场动画，直接渲染；蓝点立即可见"
        examples:
          - input: "dotsPositions=[3, 7, 12, 19, 22, 30, 37, 44, 51, 58]"
            expected: "8×8 网格中这 10 个索引位置呈蓝色实心圆，其余 54 个为浅灰空圆"
    provides_apis: []
    consumes_apis: []
    dependencies: ["1.3", "2.2"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "3.2"
    title: "闪烁动画序列（Compose 原生动画）"
    repository_type: monolith
    estimated_complexity: high
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "10 蓝点按随机顺序依次闪烁一遍"
        scenario:
          given: "学生进入记忆题页面，10 蓝点已渲染"
          when: "页面进入 0.5 秒后开始闪烁序列"
          then:
            - "App 在内存中生成本次闪烁顺序：dotsPositions 的随机排列（Fisher-Yates 洗牌）"
            - "按生成顺序依次让对应蓝点『闪烁』：颜色变深（Primary 蓝 → 黄色 #FFB300）持续 1000ms（硬编码默认值）"
            - "相邻两次闪烁之间间隔 500ms（硬编码默认值），间隔期间所有点恢复初始 Primary 蓝"
            - "10 个点全部闪烁一遍后进入复现阶段（Story 3.3）"
            - "闪烁阶段全屏禁用点击：学生任何点击都被静默忽略，不记录"
        business_rules:
          - id: "BR-1.1"
            rule: "闪烁动画必须使用 Compose 原生 API: Animatable / animate*AsState；禁止 Choreographer / withFrameNanos 帧级手动调度"
          - id: "BR-1.2"
            rule: "本次闪烁顺序须在测评内一次生成、不可复现给其他学生（每次进入题目重新洗牌）"
          - id: "BR-1.3"
            rule: "闪烁顺序须保存到内存（用于 Story 3.3 评分），但不写入结果文件 expectedSequence 字段（避免一份 result 暴露所有学生的闪烁顺序）"
            "（修订：实际写入结果，但属于该次会话独有；研究员仍可比对 answer vs correct 顺序）"
          - id: "BR-1.4"
            rule: "实测闪烁时长抖动须 ≤ ±50ms（仅作为 NFR2 验收依据；不阻塞业务逻辑）"
          - id: "BR-1.5"
            rule: "闪烁阶段顶部进度条静止显示『记忆中...』文案（无倒计时）；进入复现阶段后才开始倒计时"
        data_validation: []
        error_handling:
          - scenario: "动画 API 抛出异常（极端低端机型）"
            code: "ANIMATION_FAILED"
            message: "（用户不可见）"
            action: "记录 .diag 日志，标记本题 status=error，跳过到下一题"
        interaction:
          - trigger: "页面进入后 500ms"
            behavior: "首个蓝点开始闪烁（颜色变深 → 1000ms → 恢复 → 500ms → 下一个）"
          - trigger: "闪烁序列完成"
            behavior: "全部点恢复初始蓝色，自动进入复现阶段"
          - trigger: "闪烁阶段任何点击"
            behavior: "无视觉反馈，无声音，静默忽略"
        examples:
          - input: "10 蓝点 + 默认 1000ms/500ms"
            expected: "总闪烁时长约: 10 × 1000 + 9 × 500 = 14500ms（误差 ≤ ±50ms）"
    provides_apis: []
    consumes_apis: []
    dependencies: ["3.1"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "3.3"
    title: "学生顺序复现点击与命中判定（1.0× 半径）"
    repository_type: monolith
    estimated_complexity: high
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "学生按顺序点击 10 个点，命中判定使用 1.0× 视觉半径"
        scenario:
          given: "闪烁序列结束，进入复现阶段；当前题倒计时由 config durationMs 给定（如 60000ms）"
          when: "学生开始点击点阵"
          then:
            - "顶部进度条显示 60s 倒计时"
            - "学生每次点击系统判定命中: 触点距离最近的蓝点中心 ≤ 该蓝点视觉半径（1.0×，不放大）"
            - "未命中任何蓝点的点击不计入答案序列，不显示反馈"
            - "命中蓝点后该点呈选中态（颜色变绿 #4CAF50 + 显示已点序号 1-10）"
            - "已选中的蓝点不可重复点击（再次点击无反应）"
            - "学生完成 10 个点击或倒计时归零时本题结束"
        business_rules:
          - id: "BR-1.1"
            rule: "命中半径 = 蓝点视觉半径 × 1.0（不放大；研究场景精确性优先）"
          - id: "BR-1.2"
            rule: "命中判定使用『最近邻 + 距离阈值』算法：先找触点距离最近的点，再判断距离 ≤ 半径"
          - id: "BR-1.3"
            rule: "学生点击序列即 answer：例如点击顺序 [12, 30, 7, ...]"
          - id: "BR-1.4"
            rule: "得分 = 点击序列与 expectedSequence 在前缀位置的匹配数（不要求 10 个全对才得分）"
            "示例: expected=[3,7,12,...], answer=[3,7,19,...] → 前 2 位匹配 → 得分 2"
          - id: "BR-1.5"
            rule: "倒计时归零时未点满 10 个，已点击的部分按 BR-1.4 计分；status=done（不算 not_answered，因学生有作答动作）"
          - id: "BR-1.6"
            rule: "学生完全未点击（answer 为空）则 status=not_answered, score=0"
        data_validation:
          - field: "学生答案 answer"
            type: "string"
            required: false
            rules: "逗号分隔的网格索引序列（0-63）"
            error_message: "（内部错误）"
        error_handling:
          - scenario: "命中算法返回多个候选（理论上不应发生，因网格点位不重叠）"
            code: "HIT_AMBIGUOUS"
            message: "（用户不可见）"
            action: "选择距离最小者；记录 .diag 日志"
        interaction:
          - trigger: "复现阶段进入"
            behavior: "进度条文案从『记忆中...』变为倒计时；点阵接受点击"
          - trigger: "命中点击"
            behavior: "点变绿 + 显示已点序号（动画 100ms）"
          - trigger: "未命中点击"
            behavior: "无视觉反馈，静默"
          - trigger: "已点击的点再次被点"
            behavior: "无反应"
          - trigger: "完成 10 个点击 或 倒计时归零"
            behavior: "立即写入结果，跳转下一题，无确认弹窗"
        examples:
          - input: "expected=[3,7,12,19,22,30,37,44,51,58]，学生顺序点击 [3,7,12,19,22,30,37,44,51,58]"
            expected: "score=10, answer 与 expected 一致, status=done"
          - input: "expected=[3,7,12,...]，学生 60s 内仅点击 [3,7]"
            expected: "score=2, answer='3,7', status=done"
          - input: "学生从未点击直至超时"
            expected: "score=0, answer='', status=not_answered"
    provides_apis: []
    consumes_apis: []
    dependencies: ["3.2"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "3.4"
    title: "记忆题结果记录（点击序列、命中数、用时）"
    repository_type: monolith
    estimated_complexity: low
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "结果文件追加记忆题专属字段"
        scenario:
          given: "记忆题完成（点满 10 个 / 超时 / 未作答）"
          when: "ResultRepository.appendQuestion() 被调用"
          then:
            - "复用 Story 2.5 的字段格式: qid|type|mode|stemMs|optionsMs|answer|correct|score|status"
            - "对记忆题: type=memory, mode=all_in_one, stemMs=-, optionsMs=学生从复现阶段开始到结束的实际用时"
            - "answer 字段为学生点击序列（逗号分隔的 0-63 索引）"
            - "correct 字段为本次 expectedSequence（即随机洗牌后的闪烁顺序，逗号分隔）"
            - "score 为 Story 3.3 BR-1.4 计算结果（前缀匹配数）"
        business_rules:
          - id: "BR-1.1"
            rule: "记忆题不区分 staged 模式（即使 config 写了 staged 也按 all_in_one 处理；闪烁阶段已是天然的『题干』）"
          - id: "BR-1.2"
            rule: "optionsMs 不包含闪烁阶段时长（约 14.5 秒），仅记录复现阶段实际用时"
          - id: "BR-1.3"
            rule: "若 status=error（动画失败），写入 answer='', correct='', score=0"
        data_validation: []
        error_handling:
          - scenario: "继承 Story 2.5 的 RESULT_WRITE_FAILED 路径"
            code: "RESULT_WRITE_FAILED"
            message: "数据保存失败，请联系研究员"
            action: "同 Story 2.3 重试 3 次终止"
        examples:
          - input: "expected=[3,7,12,19,22,30,37,44,51,58], answer=[3,7,12,19,22,30,37,44,51,58], optionsMs=42300"
            expected: "Q3|memory|all_in_one|-|42300|3,7,12,19,22,30,37,44,51,58|3,7,12,19,22,30,37,44,51,58|10|done"
          - input: "expected=[3,7,12,...], answer=[3,7], optionsMs=60000（超时）"
            expected: "Q3|memory|all_in_one|-|60000|3,7|3,7,12,19,22,30,37,44,51,58|2|done"
    provides_apis: []
    consumes_apis: []
    dependencies: ["3.3", "2.5"]
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 4: 断点续答

**Epic 概述:** 实现同学号 + 同 configId 下的会话发现、已答题跳过、半完成题（仅 staged 模式）整题重做策略；达成 ≥ 95% 续答命中率与 100% 续答位置恢复目标。

**目标仓库:** monolith

```yaml
epic_id: 4
title: "断点续答"
description: |
  实现学生中途退出 / App 异常崩溃后再次扫码同一学号自动续答的能力。
  断点定义为最后一个 status=done 的题目之后的首个未完成题目；
  分阶段模式半完成题（题干已写但选项未写，理论上不应发生因 Story 2.5 BR-1.5 已避免）
  以及 status=error / not_answered 的题目均按『整题重做』策略处理（用户决策 Q4=A）。

stories:
  - id: "4.1"
    title: "同学号会话发现与续答入口路由"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "扫码后自动检测是否存在未完成会话"
        scenario:
          given: "学生扫码学号 S001，已选定 configId cog-mem-2026q3"
          when: "进入欢迎确认页前先调用 ResultRepository.findResumableSession()"
          then:
            - "扫描 /sdcard/WenJuanPro/results/ 下所有文件"
            - "匹配文件名格式: {*}_{S001}_{cog-mem-2026q3}_{*}.txt"
            - "若匹配到至少 1 份且其完成题数 < config 总题数 → 视为可续答；取最近一份（按时间戳最大）"
            - "若所有匹配文件均已完成 → 视为新会话，跳过续答提示"
            - "若无匹配 → 视为新会话"
        business_rules:
          - id: "BR-1.1"
            rule: "完成判定: 文件中所有 config 题的 qid 均出现且 status ∈ {done}"
          - id: "BR-1.2"
            rule: "未完成判定: 文件中至少有 1 题 qid 未出现，或某 qid 的 status ∈ {not_answered, error}"
          - id: "BR-1.3"
            rule: "可续答时：跳过欢迎页的『开始答题』按钮，跳转到续答提示页（AC2）"
          - id: "BR-1.4"
            rule: "续答时复用同一份结果文件，文件名（含原 sessionStart 时间戳）不变"
        data_validation: []
        error_handling:
          - scenario: "结果文件解析失败（损坏 / 半行）"
            code: "RESULT_FILE_CORRUPT"
            message: "上次会话的记录文件已损坏，将按新会话开始"
            action: "重命名损坏文件为 {原名}.corrupt.{时间戳}，开新会话"
        interaction:
          - trigger: "扫码成功后"
            behavior: "在欢迎确认页加载前调用 findResumableSession（< 1s 内完成）"
        examples:
          - input: "results/ 下有 abc_S001_cog-mem-2026q3_20260417-103045.txt（5 题中已完成 3 题）"
            expected: "返回 ResumableSession(file=该文件, completedQids=[Q1,Q2,Q3], remaining=[Q4,Q5])"
          - input: "results/ 下无匹配文件"
            expected: "返回 null，走新会话流程"

      - id: AC2
        title: "续答提示页让学生确认是否继续"
        scenario:
          given: "findResumableSession 返回非 null 的 ResumableSession"
          when: "页面进入"
          then:
            - "页面顶部显示『检测到未完成的测评』"
            - "中部显示: 学号 S001、configId、已完成 X 题 / 总 N 题、上次开始时间"
            - "底部两个按钮: 『继续上次答题』（主按钮，Primary 蓝）+ 『放弃并重新开始』（次要按钮，灰）"
            - "『继续上次答题』→ 跳转到首个未完成题目"
            - "『放弃并重新开始』→ 弹出二次确认（『将丢失上次记录，确认放弃？』）→ 确认后将旧文件重命名为 {原名}.abandoned.{时间戳}，开新会话"
        business_rules:
          - id: "BR-2.1"
            rule: "二次确认弹窗的『确认放弃』按钮放在右侧，符合 Material Design 习惯"
          - id: "BR-2.2"
            rule: "放弃旧会话不删除文件，仅重命名（保留研究员追溯能力）"
        error_handling:
          - scenario: "重命名旧文件失败（权限丢失 / 磁盘问题）"
            code: "ABANDON_RENAME_FAILED"
            message: "无法重命名旧记录，请联系研究员"
            action: "阻塞，提示研究员处理"
        interaction:
          - trigger: "页面进入"
            behavior: "无入场动画"
          - trigger: "点击『继续上次答题』"
            behavior: "立即跳转到首个未完成题目"
          - trigger: "点击『放弃并重新开始』"
            behavior: "弹出二次确认弹窗"
        examples:
          - input: "S001 + cog-mem-2026q3 + 已完成 3/5"
            expected: "续答提示页 + 两个按钮"
          - input: "点击『继续上次答题』"
            expected: "跳转到 Q4 作答页"
    provides_apis: []
    consumes_apis: []
    dependencies: ["2.5"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "4.2"
    title: "已答题跳过与从中断点继续"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0
    acceptance_criteria:
      - id: AC1
        title: "续答时跳过 status=done 的题目"
        scenario:
          given: "用户在续答提示页点击『继续上次答题』，已完成 [Q1, Q2, Q3]"
          when: "进入作答流程"
          then:
            - "作答控制器加载 config 全部题目"
            - "依次比对每题 qid 是否在已完成集合中"
            - "已完成（status=done）的 qid 直接跳过，不渲染、不计时"
            - "首个未完成 qid 即为续答起点（如 Q4）"
            - "Q4 → Q5 按正常流程进行；每题完成后追加写入同一份结果文件"
            - "全部完成后跳转完成页"
        business_rules:
          - id: "BR-1.1"
            rule: "已完成判定严格按 status=done；status=not_answered 或 status=error 的题目不视为已完成，需重做"
          - id: "BR-1.2"
            rule: "重做（not_answered / error）时该题在结果文件中以新一行覆盖旧行: 解析时按 qid 取最后一行"
            "（追加策略保留所有版本，避免破坏已落盘行）"
          - id: "BR-1.3"
            rule: "续答不重置 sessionStart 时间戳，文件名不变"
        data_validation: []
        error_handling:
          - scenario: "config 在上次会话后被研究员修改（题数增减、qid 变更）"
            code: "CONFIG_DRIFTED"
            message: "题库已发生变化，无法续答上次会话；请放弃并重新开始"
            action: "强制返回续答提示页，禁用『继续上次答题』按钮"
        interaction:
          - trigger: "续答开始"
            behavior: "无入场动画，直接进入首个未完成题目"
        examples:
          - input: "config 5 题，已完成 [Q1,Q2,Q3]，续答"
            expected: "直接进入 Q4；Q4、Q5 完成后跳转完成页"
          - input: "config 已完成 [Q1,Q2]，Q3 status=not_answered，续答"
            expected: "直接进入 Q3 重做（不跳过）"

      - id: AC2
        title: "config 漂移检测"
        scenario:
          given: "上次会话基于 config v1（5 题），本次扫描时 config 已被改为 6 题或 qid 变更"
          when: "续答前比对 config 与 result"
          then:
            - "检测 config 题数与 result 已记录题数 + 待答题数是否一致"
            - "检测 result 中的 qid 是否全部存在于当前 config"
            - "任一不一致 → 视为漂移，按 BR error_handling 处理"
        business_rules:
          - id: "BR-2.1"
            rule: "漂移检测仅做提示，不自动迁移；研究员需明确决策"
        error_handling:
          - scenario: "config 漂移"
            code: "CONFIG_DRIFTED"
            message: "题库已发生变化，无法续答上次会话；请放弃并重新开始"
            action: "在续答提示页禁用『继续上次答题』，仅保留『放弃并重新开始』"
        examples:
          - input: "上次 5 题, 当前 config 6 题"
            expected: "续答提示页禁用『继续上次答题』"
    provides_apis: []
    consumes_apis: []
    dependencies: ["4.1"]
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "4.3"
    title: "半完成题（分阶段模式）整题重做策略"
    repository_type: monolith
    estimated_complexity: low
    priority: P1
    acceptance_criteria:
      - id: AC1
        title: "status=partial 题目按整题重做"
        scenario:
          given: "结果文件中某 staged 模式题（如 Q3）status=partial（理论上不应发生，但若发生则按本规则处理）"
          when: "续答到达该 qid"
          then:
            - "题干阶段与选项阶段的两个倒计时都重新开始（与初次作答完全一致）"
            - "完成后追加新的一行（覆盖原 partial 行的语义；解析时按 qid 取最后一行）"
        business_rules:
          - id: "BR-1.1"
            rule: "Story 2.5 BR-1.5 已规定 staged 模式只在题目完整结束时才写一行；因此 partial 行理论上不会出现"
            "但若磁盘异常 / 早期版本数据 / 手动改动产生了 partial 行，本 AC 提供兜底"
          - id: "BR-1.2"
            rule: "重做不修改 sessionStart 时间戳，文件名不变"
          - id: "BR-1.3"
            rule: "结果文件中保留 partial 行 + 新的 done 行；解析按 qid 取最后一行（done 优先）"
        data_validation: []
        error_handling:
          - scenario: "重做时仍写入失败"
            code: "RESULT_WRITE_FAILED"
            message: "数据保存失败，请联系研究员"
            action: "同 Story 2.3 重试 3 次终止"
        examples:
          - input: "Q3 旧记录 status=partial（stemMs=10000, optionsMs=-, answer=, status=partial）"
            expected: "续答到 Q3 时题干 + 选项两阶段完整重做；新行 status=done 追加；解析时取新行"
    provides_apis: []
    consumes_apis: []
    dependencies: ["4.2"]
    sm_hints:
      front_end_spec: null
      architecture: null
```

---
