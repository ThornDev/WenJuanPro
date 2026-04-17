# 安全与性能

## 安全要求

**前端安全（Android App）:**

- **外部存储授权:** `MANAGE_EXTERNAL_STORAGE` 须由用户在系统设置中显式授权；App 不能在未授权状态下进入作答流程（FR1 + Story 1.2）
- **摄像头权限:** `CAMERA` 运行时权限；仅用于扫码，识别成功后立即停止预览
- **无网络:** 编译期依赖白名单守门（见 CI `verify-dependencies.sh`）；Manifest 中不声明 `INTERNET` 权限（NFR7 — 可选，但推荐以强化约束）
- **敏感日志:** `Timber.Tree` 统一过滤器：禁止写入 `studentId` 之外的个人信息；禁止写入 answer 明文（NFR9）
- **APK 签名:** 自维护 Keystore；Release 构建启用 R8 + 代码混淆

**后端安全:** N/A

**认证安全:** N/A —— 无账户。扫码即身份；"安全"在此语境下指"防止学号输入被污染"：不提供键盘输入、扫码内容严格正则匹配 `^[A-Za-z0-9_-]{1,64}$`。

**隐藏管理入口:**

- 位于测评集选择页右上角刷新按钮右侧空白区
- 触发条件：**单指连续按压 ≥ 5 秒 + 坐标精确**
- 无任何视觉反馈
- 触发后直接进入 `DiagnosticsScreen`；不需密码（现场研究员 = 管理员）

## 性能优化

**前端性能:**

- **包体积目标:** Debug APK ≤ 25 MB；Release APK ≤ 12 MB（R8 启用后，不含 assets 图片）
- **加载策略:** 冷启动 ≤ 2s（NFR3）；主要优化点是 `Application.onCreate()` 中不做任何 IO（Timber 初始化也延后到 Activity）
- **缓存策略:** Config 在进程内缓存为不可变快照；首次加载后仅在「刷新」按钮点击时重扫描
- **时序关键路径（记忆题）:**
  - 使用 `Animatable` + `animate*AsState`；不使用 `Choreographer` / `withFrameNanos`
  - 在 `MemoryQuestionScreen` 中使用 `rememberCoroutineScope()` 而非 `GlobalScope`
  - 闪烁期间不做任何 IO（答题记录写入推迟到复现阶段结束）
  - 目标：闪烁时长抖动 **≤ ±50ms**（NFR2，实测中位数）；由人工在 3 款目标机型抽样实测验收

**后端性能:** N/A

**IO 性能:**

- 所有文件 IO 走 `Dispatchers.IO`（NFR6）
- Config 扫描首次 ≤ 5s；超时展示重试（Story 1.4 BR-1.1）
- Result 追加单次 ≤ 50ms（含 fsync），连续 200 题级线性稳定

---
