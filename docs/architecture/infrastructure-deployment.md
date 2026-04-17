# 部署架构

> **WenJuanPro 无线上环境**。此节保留模板标题，但多数子项 N/A。

## 部署策略

**前端部署:** APK 通过手动侧载（U 盘 / 微信 / 内部分发链接）分发至目标 Android 设备；研究员负责首次授权 `MANAGE_EXTERNAL_STORAGE` 与 `CAMERA`。

- 平台: N/A（无云平台）
- 构建命令: `./gradlew :app:assembleRelease`
- 输出目录: `app/build/outputs/apk/release/app-release.apk`
- CDN/边缘: N/A

**后端部署:** N/A

## CI/CD 流水线

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]

jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 25
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v3
      - name: Dependency whitelist guard
        # 硬性守门：禁止引入 OkHttp / Retrofit / Firebase / 统计 SDK（NFR7）
        run: ./scripts/verify-dependencies.sh
      - name: ktlint
        run: ./gradlew ktlintCheck
      - name: Android Lint
        run: ./gradlew :app:lintDebug
      - name: JVM unit tests
        run: ./gradlew :app:testDebugUnitTest
      - name: Assemble Debug APK
        run: ./gradlew :app:assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: wenjuanpro-debug-apk
          path: app/build/outputs/apk/debug/*.apk
```

> **注:** `scripts/verify-dependencies.sh` 在 `./gradlew :app:dependencies` 输出中对 `okhttp|retrofit|firebase|mlkit|gms|umeng|bugly|sentry|hockeyapp|matrix` 等关键字做 grep 阻断。

## 环境

| 环境       | 前端 URL | 后端 URL | 用途                                      |
|------------|----------|----------|-------------------------------------------|
| 开发       | N/A      | N/A      | 研究员/开发者连接设备真机本地运行 Debug APK |
| 预发       | N/A      | N/A      | 研究员在自己设备上先跑一次样例题库验收      |
| 生产       | N/A      | N/A      | 现场测评（Release APK 由研究团队自行分发）  |

---
