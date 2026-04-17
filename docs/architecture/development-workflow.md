# 开发工作流

## 本地开发配置

### 前置条件

```bash
# JDK 17 (AGP 8.5.2 + Kotlin 1.9.24 要求)
java -version    # expected: openjdk 17.x

# Android Studio（Koala 2024.1.1 或更新）或命令行：
# Android SDK (API 26 ~ API 34) + Platform Tools + Build Tools 34.0.0

# 可选：ktlint 本地预提交（CI 也会跑一次）
brew install ktlint
```

### 初始配置

```bash
# 克隆并进入仓库
git clone <repo-url> WenJuanPro && cd WenJuanPro

# 拉取 Gradle Wrapper 并首次构建（下载依赖）
./gradlew --version
./gradlew :app:dependencies

# 配置测试设备（真机 or 模拟器 API 26+）
adb devices
# 若使用真机：
# 1) 开启开发者模式 + USB 调试
# 2) 首次安装 APK 后手动授权 "全部文件访问"
# 3) 推送示例 config 与 asset：
adb shell mkdir -p /sdcard/WenJuanPro/config /sdcard/WenJuanPro/assets /sdcard/WenJuanPro/results
adb push docs/samples/cog-mem-2026q3.txt /sdcard/WenJuanPro/config/
```

### 开发命令

```bash
# 启动所有服务 —— N/A（单 App，无服务端）
# 仅启动前端 —— 见下
# 仅启动后端 —— N/A

# 构建 Debug APK
./gradlew :app:assembleDebug

# 安装到已连接的设备
./gradlew :app:installDebug

# 运行 JVM 单元测试
./gradlew :app:testDebugUnitTest

# 运行 Android Instrumented 测试（需连接设备）
./gradlew :app:connectedDebugAndroidTest

# Lint + ktlint 检查
./gradlew ktlintCheck lint

# 清理
./gradlew clean
```

## 环境配置

### 必需的环境变量

```bash
# 前端 (.env.local) —— N/A（Android App 不读 .env）
# 后端 (.env)        —— N/A
# 共享                —— N/A

# APK 签名（仅 Release 构建期间使用，由 CI 或本地构建机维护）:
export WJP_KEYSTORE_PATH=/path/to/wenjuanpro.keystore
export WJP_KEYSTORE_PASSWORD=******
export WJP_KEY_ALIAS=wenjuanpro
export WJP_KEY_PASSWORD=******
```

App 运行时没有"环境变量"概念；任何运行时可调参数（如 `flashDurationMs` 默认值）均在 Kotlin 源码中硬编码，或在 config TXT 中显式声明（题目级覆盖）。

---
