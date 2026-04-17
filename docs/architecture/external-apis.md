# 外部 API

**N/A —— App 不调用任何外部 API。**

以下系统级 API 为架构依赖（非网络 API）：

- `Settings.Secure.ANDROID_ID`（读取 SSAID 作为 deviceId）
- `Environment.isExternalStorageManager()` / `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`（权限检测与请求）
- `CameraX` + `ZXing`（扫码；纯本地 CPU 解码，不依赖 Google Play Services）
- Android Keystore 用于 APK 签名（仅构建期，非运行时）

---
