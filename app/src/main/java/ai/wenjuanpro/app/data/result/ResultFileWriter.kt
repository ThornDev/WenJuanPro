package ai.wenjuanpro.app.data.result

import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * Low-level wrapper around a single result file's [FileOutputStream].
 *
 * Every [appendLine] call executes the atomic sequence:
 * **write → flush → fd.sync()** to guarantee the line is persisted
 * even if the process crashes immediately after.
 *
 * All [IOException]s are converted to [WriteFailedException].
 */
class ResultFileWriter(private val fos: FileOutputStream) {

    private val writer: BufferedWriter =
        BufferedWriter(OutputStreamWriter(fos, Charsets.UTF_8))

    fun writeHeader(headerBlock: String) {
        try {
            writer.write(headerBlock)
            writer.flush()
            fos.fd.sync()
        } catch (e: IOException) {
            throw WriteFailedException(
                code = "HEADER_WRITE_FAILED",
                message = "Failed to write header: ${e.message}",
                cause = e,
            )
        }
    }

    fun appendLine(line: String) {
        try {
            writer.write(line + "\n")
            writer.flush()
            fos.fd.sync()
        } catch (e: IOException) {
            throw WriteFailedException(
                code = if (e.message?.contains("sync", ignoreCase = true) == true) {
                    "FSYNC_FAILED"
                } else {
                    "RESULT_WRITE_FAILED"
                },
                message = "Failed to append line: ${e.message}",
                cause = e,
            )
        }
    }

    fun close() {
        runCatching { writer.close() }
        runCatching { fos.close() }
    }
}
