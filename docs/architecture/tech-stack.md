# 技术栈

此表为整个 WenJuanPro 项目的**技术唯一真实来源**。Dev Agent 必须严格依从此版本，任何偏差须经 Architect 评审。

## 技术栈表

| 类别              | 技术                                    | 版本                | 用途                                       | 选择理由                                                                                 |
|-------------------|-----------------------------------------|---------------------|--------------------------------------------|------------------------------------------------------------------------------------------|
| 客户端语言        | Kotlin                                  | 1.9.24              | App 全部业务代码                           | Android 官方主推；与 Compose 深度集成；协程与 Flow 原生支持                                |
| UI 框架           | Jetpack Compose                         | BOM 2024.09.02      | 声明式 UI                                  | 贴合 MVI 单向流；`Animatable` 满足 ±50ms 时序抖动；与 Material 3 无缝衔接                 |
| Compose Compiler  | Compose Compiler Gradle Plugin          | 1.5.14              | Compose 代码生成                           | 匹配 Kotlin 1.9.24                                                                       |
| UI 组件库         | Material 3 (Compose)                    | BOM 2024.09.02      | Scaffold / Card / Button / Icon            | PRD 指定默认浅色主题；零品牌诉求                                                           |
| 导航              | AndroidX Navigation-Compose             | 2.8.0               | Screen 路由 + 返回键策略                   | 与 Compose NavHost 无缝；支持 `NavBackStackEntry` 上的 SavedStateHandle                    |
| 状态管理          | StateFlow + MVI                         | Kotlin 1.9 内置     | ViewModel 持有 `StateFlow<UiState>`        | 无需引入额外库；可在 ViewModel 中用 `update { }` 安全修改                                   |
| 依赖注入          | Hilt                                    | 2.52                | Repository / UseCase / ViewModel 图        | Android 官方推荐；编译期校验；Compose Navigation Hilt 模块直接注入 ViewModel                |
| 协程              | kotlinx.coroutines                      | 1.8.1               | IO 调度 + 动画驱动的 `launch` 上下文       | `Dispatchers.IO` 执行文件 IO；`viewModelScope` 管理 UI 生命周期                              |
| 序列化            | kotlinx.serialization                   | 1.7.2               | 内存数据结构序列化（诊断日志/测试替身）     | 官方库；不需要额外反射                                                                      |
| 文件 IO           | `java.nio.file` + Okio 3.9.0 (可选)     | Okio 3.9.0          | UTF-8 行式读写 + fsync                     | `Okio.BufferedSink.flush()` + `FileChannel.force(false)` 保证 fsync；API 简洁可测试         |
| 扫码              | CameraX + ZXing Android Embedded        | CameraX 1.3.4 / ZXing-Android-Embedded 4.3.0 | 后置摄像头预览 + 二维码识别 | 纯本地解码，不依赖 Google Play Services，完全贴合离线定位；CameraX 提供预览帧，ZXing 做纯 CPU 解码 |
| 动画              | Compose Animation（高层 API）           | 随 Compose BOM      | 闪烁 / 倒计时进度条 / 阶段切换淡入淡出      | 仅用 `Animatable`、`animate*AsState`、`InfiniteTransition`；禁用 Choreographer              |
| 日志              | Timber + 自定义 FileTree                 | Timber 5.0.1        | 分级日志 + 落盘到 `.diag/app.log`          | Timber 轻量；自定义 FileTree 仅记录非答题明文的诊断信息                                     |
| CSS 框架          | N/A                                     | —                   | —                                          | 无 Web UI                                                                                   |
| 前端语言          | N/A（使用 Kotlin + Compose）             | —                   | —                                          | 无 Web 前端                                                                                 |
| 后端语言          | **N/A**                                 | —                   | —                                          | 全离线 App，无后端                                                                          |
| 后端框架          | **N/A**                                 | —                   | —                                          | 同上                                                                                        |
| API 风格          | **N/A**                                 | —                   | —                                          | 无 API；"契约"形态为 TXT Schema（见下文「文件存储 Schema」）                                |
| 数据库            | **N/A**                                 | —                   | —                                          | 单一事实源为 TXT 文件，刻意不引入 Room/SQLite                                                |
| 缓存              | 进程内内存缓存（`StateFlow` / `val`）    | —                   | Config 解析结果缓存                        | 进程生命周期内的不可变快照；页面重进时按需读取                                                |
| 文件存储          | Android 外部存储（`MANAGE_EXTERNAL_STORAGE`） | 系统 API            | config / assets / results / .diag          | PRD 强约束：固定路径 `/sdcard/WenJuanPro/`                                                 |
| 认证              | N/A（扫码即学号）                        | —                   | —                                          | 无账户体系；二维码内容 = studentId                                                            |
| 客户端测试 (JVM)  | JUnit 4 + MockK + kotlinx-coroutines-test | JUnit 4.13.2 / MockK 1.13.13 | 解析器 / 状态机 / 评分器单测     | 纯 JVM 可执行；不依赖 Android framework                                                       |
| 客户端测试 (Android) | Robolectric + AndroidX Test            | Robolectric 4.13    | Repository / 权限分支 / SSAID 获取集成测试 | Robolectric 在 JVM 中模拟 Android runtime，避免每次跑 instrumented test                        |
| UI 测试           | Compose UI Test + Espresso              | 随 Compose BOM / Espresso 3.6.1 | 1-2 条「扫码 → 作答 → 落盘 → 续答」主路径 UI 测试 | 关键路径用 instrumented test 在真机/模拟器跑；记忆题时序不纳入自动化（依赖人工实测）           |
| E2E 测试          | **N/A**                                 | —                   | —                                          | 单 Activity + 单进程 App，UI 测试已覆盖关键 E2E 路径                                         |
| 构建工具          | Gradle (KTS)                            | 8.7                 | Android 项目构建                           | Android Studio 默认；AGP 8.5.2                                                                |
| 打包工具          | Gradle + R8                             | 随 AGP              | APK 打包 + 代码缩减                         | Release 构建启用 R8 缩减 + 代码混淆；Debug 构建关闭                                           |
| IaC 工具          | **N/A**                                 | —                   | —                                          | 无服务端基础设施                                                                               |
| CI/CD             | GitHub Actions（或 Gitea Runner）        | —                   | Lint + 单测 + APK 打包                      | 不部署线上环境；仅产出 Debug / Release APK 作为工件                                            |
| 监控              | Timber 本地日志 + 诊断页                 | —                   | 崩溃 stacktrace 落盘 `.diag/`              | 无 Firebase / Sentry 等外发上报，保证离线原则                                                 |
| 日志             | Timber FileTree → `/sdcard/WenJuanPro/.diag/app.log` | —       | 诊断与崩溃现场                             | 日志不得包含答题明文（NFR9）                                                                   |

**扫码引擎决策（v1.1 锁定）:** 单押 ZXing Android Embedded 4.3.0；**不引入 ML Kit Barcode Scanning**，原因：ML Kit 依赖 Google Play Services 动态下发模型，与本 App"完全离线、零 Google 依赖"定位冲突。CameraX 仅负责摄像头预览帧获取，ZXing 做纯 CPU 解码。

**图像加载:** Coil 2.7.0（可选，仅当 `assets/*.png` 单张 > 200KB 时启用；当前 MVP 图片较小，直接用 `BitmapFactory` 即可）。

---
