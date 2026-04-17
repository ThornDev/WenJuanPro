package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.io.FileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoopResultRepository
    @Inject
    constructor(
        private val fileSystem: FileSystem,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ResultRepository {
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
