package ai.wenjuanpro.app.data.result

import java.io.IOException

/**
 * Thrown when a result-file write operation fails (write / flush / fsync).
 *
 * @param code  Machine-readable error code (e.g. `RESULT_WRITE_FAILED`, `FSYNC_FAILED`).
 */
class WriteFailedException(
    val code: String = "RESULT_WRITE_FAILED",
    message: String = code,
    cause: Throwable? = null,
) : IOException(message, cause)
