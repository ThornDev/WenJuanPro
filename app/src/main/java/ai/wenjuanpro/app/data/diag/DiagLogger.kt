package ai.wenjuanpro.app.data.diag

import ai.wenjuanpro.app.core.io.FileSystem
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Appends diagnostic (non-sensitive) log entries to
 * `/sdcard/WenJuanPro/.diag/app.log`.
 *
 * Format: `[ISO-8601] ERROR {code}: {message}`
 *
 * **MUST NOT** record answer, score, or studentId (NFR9).
 */
@Singleton
open class DiagLogger
    @Inject
    constructor(
        private val fileSystem: FileSystem,
    ) {
    open fun log(code: String, message: String) {
        try {
            val dir = File(DIAG_DIR)
            if (!dir.isDirectory) {
                fileSystem.mkdirs(DIAG_DIR)
            }
            val timestamp = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val entry = "[$timestamp] ERROR $code: $message\n"
            FileOutputStream(File(DIAG_DIR, LOG_FILE), true).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { w ->
                    w.write(entry)
                    w.flush()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "DiagLogger.log failed; code=%s", code)
        }
    }

    companion object {
        const val DIAG_DIR = "/sdcard/WenJuanPro/.diag"
        const val LOG_FILE = "app.log"
    }
}
