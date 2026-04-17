# 源码目录

```text
WenJuanPro/
├── .github/
│   └── workflows/
│       └── ci.yml                    # Lint + JVM 单测 + APK 打包
├── app/                              # 唯一 Android 模块
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # MANAGE_EXTERNAL_STORAGE / CAMERA / 锁 portrait
│       │   ├── java/ai/wenjuanpro/app/
│       │   │   ├── WenJuanProApp.kt                    # Application + @HiltAndroidApp
│       │   │   ├── MainActivity.kt                     # 单 Activity + setContent { WenJuanProApp() }
│       │   │   ├── ui/
│       │   │   │   ├── theme/ (Theme.kt / Color.kt / Typography.kt)
│       │   │   │   ├── components/ (CountdownBar / OptionCard / DotGrid / DotCell / ...)
│       │   │   │   └── screens/ (permission / configlist / scan / welcome / resume / question / complete / diagnostics)
│       │   │   ├── feature/
│       │   │   │   ├── permission/PermissionViewModel.kt
│       │   │   │   ├── configlist/ConfigListViewModel.kt
│       │   │   │   ├── scan/ScanViewModel.kt
│       │   │   │   ├── welcome/WelcomeViewModel.kt
│       │   │   │   ├── resume/ResumeViewModel.kt
│       │   │   │   ├── question/QuestionViewModel.kt
│       │   │   │   ├── complete/CompleteViewModel.kt
│       │   │   │   └── diagnostics/DiagnosticsViewModel.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/ (Config.kt / Question.kt / Session.kt / ResultRecord.kt / ...)
│       │   │   │   ├── usecase/ (LoadConfigs / StartSession / DetectResumable / AppendResult / Score* / Abandon)
│       │   │   │   └── fsm/ (QuestionFsm.kt / ResumeFsm.kt — 表驱动状态机)
│       │   │   ├── data/
│       │   │   │   ├── config/ (ConfigRepository.kt / ConfigRepositoryImpl.kt)
│       │   │   │   ├── result/ (ResultRepository.kt / ResultRepositoryImpl.kt)
│       │   │   │   ├── permission/ (PermissionRepository.kt / PermissionRepositoryImpl.kt)
│       │   │   │   ├── device/ (DeviceIdProvider.kt / DeviceIdProviderImpl.kt)
│       │   │   │   └── parser/ (ConfigParser.kt / ResultFormatter.kt / ResultParser.kt)
│       │   │   ├── core/
│       │   │   │   ├── io/ (FileSystem.kt / OkioFileSystem.kt)
│       │   │   │   ├── concurrency/ (Dispatchers.kt + @IoDispatcher/@MainDispatcher 注解)
│       │   │   │   ├── time/ (Clock.kt / SystemClock.kt)
│       │   │   │   └── log/ (Logger.kt / FileTree.kt)
│       │   │   └── di/
│       │   │       ├── DataModule.kt                    # Repository 绑定
│       │   │       ├── DispatchersModule.kt             # @IoDispatcher / @MainDispatcher
│       │   │       ├── ParserModule.kt
│       │   │       └── SessionModule.kt                 # @ActivityRetainedScoped SessionStateHolder
│       │   └── res/
│       │       ├── values/ (strings.xml / themes.xml)
│       │       └── ...
│       ├── test/                     # JVM 单测（Robolectric + JUnit4 + MockK）
│       │   └── java/ai/wenjuanpro/app/
│       │       ├── data/parser/ConfigParserTest.kt
│       │       ├── data/parser/ResultFormatterTest.kt
│       │       ├── data/parser/ResultParserTest.kt
│       │       ├── domain/fsm/QuestionFsmTest.kt
│       │       ├── domain/usecase/ScoreSingleChoiceUseCaseTest.kt
│       │       ├── domain/usecase/ScoreMemoryUseCaseTest.kt
│       │       ├── domain/usecase/DetectResumableSessionUseCaseTest.kt
│       │       └── data/result/ResultRepositoryImplTest.kt   # 用假 FileSystem 验证原子追加
│       └── androidTest/              # Instrumented / Compose UI 测试
│           └── java/ai/wenjuanpro/app/
│               ├── ui/ScanToAppendE2ETest.kt              # 扫码→单选→落盘→续答主路径
│               └── ui/MemoryQuestionRenderTest.kt         # 记忆题 UI 渲染（时序抖动不自动化）
├── build.gradle.kts                  # 根
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml         # 版本目录（锁版本）
├── docs/
│   ├── project-brief.md
│   ├── prd.md
│   ├── front-end-spec.md
│   └── architecture.md               # 本文件
├── .gitignore
├── .editorconfig                     # ktlint 对齐
└── README.md
```

**关键命名约定:**

- `package`: `ai.wenjuanpro.app.{ui|feature|domain|data|core|di}.…`
- `applicationId`: `ai.wenjuanpro.app`
- `versionCode` / `versionName`: 由 `gradle/libs.versions.toml` 中心化管理；每次 Release 手动递增

---
