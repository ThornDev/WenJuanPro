package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.core.BuildConfigProvider
import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.data.diag.DiagLogger
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.Session
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File-backed implementation of [ResultRepository].
 *
 * - [openSession]: creates the result file + writes header (or opens in append mode for resume).
 * - [append]: atomically appends a pipe-delimited data line (write → flush → fsync).
 * - [findResumable]: scans the results directory for incomplete files.
 */
@Singleton
class ResultRepositoryImpl
    @Inject
    constructor(
        private val fileSystem: FileSystem,
        private val deviceIdProvider: DeviceIdProvider,
        private val buildConfigProvider: BuildConfigProvider,
        private val diagLogger: DiagLogger,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ResultRepository {

    @Volatile
    private var resultFileWriter: ResultFileWriter? = null

    @Volatile
    private var currentSession: Session? = null

    // ── startSession (legacy contract from Story 2.2) ──────────────────────

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
                fileSystem.listFiles(resultsDir, ".txt")
                    .filter { path -> path.substringAfterLast('/').startsWith(prefix) }
            matches.any { path ->
                val content = runCatching { String(fileSystem.readBytes(path), Charsets.UTF_8) }.getOrNull()
                    ?: return@any false
                val qids = ResultFileParser.parseCompletedQids(content) ?: return@any false
                qids.size in 1 until totalQuestions
            }
        }

    // ── openSession / append / closeSession (Story 2.5) ────────────────────

    override suspend fun openSession(session: Session) {
        withContext(ioDispatcher) {
            resultFileWriter?.close()
            resultFileWriter = null
            currentSession = null

            fileSystem.mkdirs(resultsDir)
            val filePath = resultsDir + session.resultFileName
            val fileExists = fileSystem.exists(filePath)

            val fos = fileSystem.openAppend(filePath)
            val writer = ResultFileWriter(fos)

            if (!fileExists) {
                try {
                    val header = SessionHeaderSerializer.serialize(
                        session,
                        buildConfigProvider.appVersion(),
                    )
                    writer.writeHeader(header)
                } catch (e: WriteFailedException) {
                    writer.close()
                    fileSystem.deleteIfExists(filePath)
                    throw e
                }
            }

            resultFileWriter = writer
            currentSession = session
        }
    }

    override suspend fun append(record: ResultRecord): Result<Unit> =
        withContext(ioDispatcher) {
            val writer = resultFileWriter
                ?: return@withContext Result.failure(
                    WriteFailedException(
                        code = "RESULT_WRITE_FAILED",
                        message = "No active session",
                    ),
                )
            try {
                val line = ResultRecordSerializer.serialize(record)
                writer.appendLine(line)
                Result.success(Unit)
            } catch (e: WriteFailedException) {
                diagLogger.log(e.code, e.message ?: e.code)
                Result.failure(e)
            }
        }

    override fun closeSession() {
        resultFileWriter?.close()
        resultFileWriter = null
        currentSession = null
    }

    // ── findResumable (Story 2.5 / AC2) ────────────────────────────────────

    override suspend fun findResumable(
        studentId: String,
        configId: String,
    ): ResumeCandidate? =
        withContext(ioDispatcher) {
            val deviceId = deviceIdProvider.ssaid() ?: return@withContext null
            if (!fileSystem.exists(resultsDir)) return@withContext null

            val prefix = "${deviceId}_${studentId}_${configId}_"
            val matches = fileSystem.listFiles(resultsDir, ".txt")
                .filter { it.substringAfterLast('/').startsWith(prefix) }

            if (matches.isEmpty()) return@withContext null

            val session = currentSession
            val totalQuestions = session?.config?.questions?.size ?: Int.MAX_VALUE

            val candidates = matches.mapNotNull { path ->
                try {
                    val content = String(fileSystem.readBytes(path), Charsets.UTF_8)
                    val qids = ResultFileParser.parseCompletedQids(content)
                    if (qids == null) {
                        Timber.w("Corrupt result file, skipping: %s", path)
                        return@mapNotNull null
                    }
                    if (qids.size >= totalQuestions) return@mapNotNull null
                    val fileName = path.substringAfterLast('/')
                    ResumeCandidate(
                        resultFileName = fileName,
                        completedQids = qids,
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to read result file: %s", path)
                    null
                }
            }

            // Pick latest by timestamp in filename (last segment before .txt)
            candidates.maxByOrNull { it.resultFileName }
        }

    /** Visible for testing – allows overriding the results directory. */
    internal var resultsDir: String = RESULTS_DIR

    companion object {
        const val RESULTS_DIR = "/sdcard/WenJuanPro/results/"
        const val TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss"
    }
}
