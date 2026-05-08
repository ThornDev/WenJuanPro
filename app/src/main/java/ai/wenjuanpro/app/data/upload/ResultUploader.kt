package ai.wenjuanpro.app.data.upload

import java.io.File

/**
 * Sends a finished result file to the upstream collection service.
 */
interface ResultUploader {
    /**
     * @return Result.success(Unit) on a 2xx response, Result.failure otherwise.
     *   Implementations are expected to be cancellation-friendly.
     */
    suspend fun upload(file: File): Result<Unit>
}
