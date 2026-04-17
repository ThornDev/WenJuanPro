package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.Session
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Story 2.5 之前的中转实现：startSession / hasIncompleteResult 保留 Story 2.2 的契约（不写文件，
 * 仅按命名规则返回会话文件名 + 通过 FileSystem 探测续答候选）；新增的 openSession / append /
 * findResumable 走纯内存 MutableList，在 Story 2.5 中将被 ResultRepositoryImpl（真正的 TXT 落盘）取代。
 */
@Singleton
class InMemoryResultRepository
    @Inject
    constructor(
        private val fileSystem: FileSystem,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ResultRepository {
        private val mutex = Mutex()
        private var currentSession: Session? = null
        private val records: MutableList<ResultRecord> = mutableListOf()

        val recordedResults: List<ResultRecord>
            get() = records.toList()

        override suspend fun startSession(
            deviceId: String,
            studentId: String,
            configId: String,
            sessionStartMs: Long,
        ): StartSessionResult =
            withContext(ioDispatcher) {
                val stamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date(sessionStartMs))
                val fileName = "${deviceId}_${studentId}_${configId}_$stamp.txt"
                StartSessionResult.Success(sessionFileName = fileName)
            }

        override suspend fun hasIncompleteResult(
            deviceId: String,
            studentId: String,
            configId: String,
            totalQuestions: Int,
        ): Boolean =
            withContext(ioDispatcher) {
                val prefix = "${deviceId}_${studentId}_${configId}_"
                val matches =
                    fileSystem.listFiles(RESULTS_DIR, ".txt")
                        .filter { path -> path.substringAfterLast('/').startsWith(prefix) }
                matches.any { path ->
                    val rows = runCatching { fileSystem.readBytes(path) }.getOrNull()?.toLineCount() ?: 0
                    rows in 1 until totalQuestions
                }
            }

        override suspend fun openSession(session: Session) {
            mutex.withLock {
                currentSession = session
                records.clear()
            }
        }

        override suspend fun append(record: ResultRecord): Result<Unit> =
            mutex.withLock {
                runCatching { records.add(record) }.map { }
            }

        override suspend fun findResumable(
            studentId: String,
            configId: String,
        ): ResumeCandidate? = null

        private fun ByteArray.toLineCount(): Int {
            if (isEmpty()) return 0
            var count = 1
            for (b in this) if (b == '\n'.code.toByte()) count++
            return count
        }

        companion object {
            const val RESULTS_DIR = "/sdcard/WenJuanPro/results/"
            const val TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss"
        }
    }
