# 后端架构

> **N/A —— 无后端服务。**

WenJuanPro 的"后端"在本项目语境下是 **设备内的 IO + 解析 + 评分层**，已在上文「组件 / UseCase / Repository」中完整定义。此节保留标题以对齐模板，具体内容参见：

- 服务架构 → 见「组件」一节的 UseCase / Repository 拆分
- 数据库架构 → 见「文件存储 Schema」（TXT 行式格式）
- 认证与授权 → **N/A**（无账户）；权限模型改为 Android 运行时权限 + `MANAGE_EXTERNAL_STORAGE`，见「安全与性能 → 安全要求」

## 数据访问层（Repository 实现要点）

```kotlin
class ResultRepositoryImpl @Inject constructor(
    private val fileSystem: FileSystem,
    private val formatter: ResultFormatter,
    private val parser: ResultParser,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ResultRepository {

    override suspend fun append(record: ResultRecord): Result<Unit> =
        runCatching {
            withContext(ioDispatcher) {
                val line = formatter.formatRecord(record) + "\n"
                val path = currentSessionPath
                    ?: error("Session not opened; call openSession() first")
                fileSystem.appendAtomically(path, line.toByteArray(Charsets.UTF_8))
                fileSystem.fsync(path)
            }
        }

    // appendAtomically 内部：RandomAccessFile/FileChannel.write + FileChannel.force(false)
}
```

**原子追加的关键约束（见 NFR12）:**

1. 整行（含尾 `\n`）先在内存拼装，一次性 write；
2. write 返回后调用 `FileChannel.force(false)` 完成 fsync；
3. 不使用 `BufferedWriter` 持有进程级缓冲（避免 App 被杀时尾部丢失）；
4. 目录不存在时自动 `mkdirs()`，并落 `.diag` 日志。

---
