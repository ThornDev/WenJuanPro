# 编码规范

## 关键全栈规则

- **依赖白名单:** 永远不要引入 OkHttp / Retrofit / Firebase / Bugly / Umeng / Sentry / Matrix 等带网络或上报的库；新依赖须先更新 `scripts/verify-dependencies.sh` 白名单
- **无本地数据库:** 永远不要引入 Room / SQLite；持久化仅走 `ConfigRepository` / `ResultRepository`
- **IO 仅在 Repository:** UI / ViewModel / UseCase 不得直接调用 `java.io.File` / `Okio`；所有文件操作走 `FileSystem` 接口
- **时序代码仅用 Compose 高层 API:** 禁用 `Choreographer` / `Handler.postDelayed` / `withFrameNanos`（`InfiniteTransition` 内部封装除外）
- **Intent → Reducer 单向流:** ViewModel 永远不要直接调用 UI；UI → `onIntent(Intent)`；VM → `_uiState.update { }` → UI 重组
- **不可变 Config / Question / Session:** 这些 `data class` 一旦解析完成即只读；任何"改题目"需要重新解析
- **协程作用域:** VM 用 `viewModelScope`；`Application` 作用域协程**禁止**（可能泄漏进程生命）；一次性 UseCase 使用 `withContext(ioDispatcher)` 切线程
- **日志内容过滤:** 永远不要 `Timber.d(answer)`；仅打印题号、题型、状态枚举（NFR9）
- **Hilt Scope:** `SessionStateHolder` 使用 `@ActivityRetainedScoped`；Repository 使用 `@Singleton`
- **UTF-8 无 BOM:** 读写 TXT 统一使用 `Charsets.UTF_8`；写入不添加 BOM；读取遇 BOM 告警但不阻塞
- **错误用户文案 vs 日志:** 用户看到的文案走 `strings.xml` + 错误码表；技术细节仅进 Timber / `.diag/app.log`

## 命名规范

| 元素           | UI (Compose)                     | Domain / Data (Kotlin)      | 示例                                           |
|----------------|----------------------------------|------------------------------|------------------------------------------------|
| Composable     | PascalCase，不带 `Screen` 时加后缀 | -                            | `QuestionScreen`, `CountdownBar`               |
| ViewModel      | PascalCase + `ViewModel`         | -                            | `QuestionViewModel`                            |
| UseCase        | -                                | PascalCase + `UseCase`       | `AppendResultUseCase`                          |
| Repository 接口 | -                                | PascalCase + `Repository`    | `ResultRepository`                              |
| Repository 实现 | -                                | 接口名 + `Impl`              | `ResultRepositoryImpl`                          |
| DTO / Data class | -                              | PascalCase                   | `Config`, `ResultRecord`                       |
| 枚举            | -                                | PascalCase + 值 UPPER        | `ResultStatus.DONE`                            |
| Config 文件名    | -                                | kebab-case                   | `cog-mem-2026q3.txt`                           |
| Result 文件名    | -                                | `{dev}_{sid}_{cid}_{ts}.txt` | `abc123_S001_cog-mem-2026q3_20260417-103045.txt` |
| 错误码          | -                                | UPPER_SNAKE_CASE             | `CONFIG_HEADER_MISSING`, `RESULT_WRITE_FAILED` |
| Intent / Effect | -                                | PascalCase（在 sealed 中）    | `Intent.Submit`, `Effect.NavigateNext`         |

---
