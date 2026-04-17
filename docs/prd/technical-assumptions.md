# 技术假设

## 仓库结构: Monolith

单仓库（Polyrepo: 否；Multi-repo: 否）。整个 App 由单个 Android Studio 工程承载，初期单模块 `app/`，待第二阶段（更多认知范式题型）再按 feature module 拆分。**目标平台单一（仅 Android）**，无后端/前端/移动多仓协同需求；故无 `repository-details` 表。

## 服务架构

**单体 Android App，无服务端**。App 内部采用 MVI（Model-View-Intent）单向数据流，与 Compose 声明式 UI 天然契合。IO 层抽象为两个 Repository：
- `ConfigRepository`：负责 `/sdcard/WenJuanPro/config/*.txt` 的发现、解析、schema 校验、缓存。
- `ResultRepository`：负责 `/sdcard/WenJuanPro/results/*.txt` 的发现（用于断点续答）、追加写入、原子性保证。

UI 状态由 ViewModel 持有，数据流向：`Intent → ViewModel → State → Composable`；副作用（写文件、扫码启动、计时器）封装在 `UseCase` / `Service` 层。**无本地数据库**（无 Room / SQLite），单一事实源为 TXT 文件，避免双写不一致。

## 测试要求

**单元测试 + 集成测试**（不强制完整 E2E 金字塔）：

- **单元测试（必需）**：`ConfigRepository` 的 TXT 解析器与 schema 校验、`ResultRepository` 的命名生成与追加写入逻辑、断点续答状态机（含半完成题"整题重做"路径）、记忆题命中判定算法。覆盖率目标核心模块 ≥ 80%。
- **集成测试（必需）**：使用 Robolectric 或 instrumented test 验证文件 IO 的真实读写、SSAID 获取、`MANAGE_EXTERNAL_STORAGE` 权限检测分支。
- **UI 测试（关键路径）**：1-2 条 Espresso/Compose Test 覆盖"扫码 → 单选 → 落盘 → 续答"主路径；记忆题闪烁动画**不**纳入自动化（依赖人工时序实测）。
- **手动测试便利方法**：诊断页提供"模拟扫码"输入框（仅 debug build 启用）、"清空 results"按钮、"导出诊断日志"按钮，便于研究员侧自验。

## 其他技术假设与要求

- **语言/框架**：Kotlin（≥ 1.9）+ Jetpack Compose（Material 3）+ Compose Animation；Gradle KTS 构建。
- **扫码**：CameraX + ML Kit Barcode Scanning（首选）；ZXing 作为兜底备选（视 ML Kit 在低端机型上的稳定性而定）。
- **动画时序**：记忆题闪烁与倒计时进度条**仅使用** Compose 原生动画 API（`Animatable` / `animate*AsState` / `InfiniteTransition` / `withFrameNanos` 中的高层封装）；**禁止使用** `Choreographer` / `View.postOnAnimation` 等帧级手动调度（与 brief v0.2 决策一致）。
- **协程**：Kotlin Coroutines + Flow；IO 操作走 `Dispatchers.IO`，UI 状态更新走 `Dispatchers.Main`。
- **依赖注入**：Hilt（轻量场景下的 ServiceLocator 也可，由 Architect 决定）。
- **序列化**：`kotlinx.serialization` 处理内存数据结构；TXT 解析器手写（避免引入 CSV 库带来的过度抽象）。
- **日志**：Timber + 自定义 FileTree 写入 `/sdcard/WenJuanPro/.diag/app.log`（仅记录非答题明文的诊断信息）。
- **网络**：**严禁**引入 OkHttp / Retrofit / Firebase / 任何统计 SDK；CI 中通过依赖白名单守门。
- **APK 签名**：使用研究团队自维护的 Keystore，APK 通过 U 盘/微信/内部链接分发；最低支持 API 26，目标 API 34。
- **CI/CD**：GitHub Actions（或团队自建 Gitea Runner）执行 Lint + 单元测试 + APK 打包；不部署任何线上环境。
- **错误上报**：本地落盘 stacktrace 到 `.diag/`，不外发；崩溃后下次启动时在诊断页提示"上次会话异常退出"。
- **设备 ID 稳定性**：使用 `Settings.Secure.ANDROID_ID`（SSAID），研究员需在测评周期内避免对设备执行恢复出厂设置。

---
