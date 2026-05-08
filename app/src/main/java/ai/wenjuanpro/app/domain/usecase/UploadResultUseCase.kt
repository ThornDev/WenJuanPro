package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.data.upload.ResultUploader
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads a finished result file with bounded retries. The first attempt
 * fires immediately; failures wait [INITIAL_BACKOFF_MS] (doubling each
 * retry) before the next try, up to [MAX_ATTEMPTS] in total.
 *
 * The [onAttempt] callback fires once per attempt with the 1-based attempt
 * number so a UI can show "上传中… (2/3)".
 */
@Singleton
class UploadResultUseCase
    @Inject
    constructor(
        private val uploader: ResultUploader,
    ) {
        suspend operator fun invoke(
            file: File,
            onAttempt: (attempt: Int) -> Unit = {},
        ): Result<Unit> {
            var lastFailure: Throwable? = null
            var backoff = INITIAL_BACKOFF_MS
            for (attempt in 1..MAX_ATTEMPTS) {
                onAttempt(attempt)
                val result = uploader.upload(file)
                if (result.isSuccess) {
                    Timber.d("result upload ok attempt=%d file=%s", attempt, file.name)
                    return result
                }
                lastFailure = result.exceptionOrNull()
                Timber.w(
                    "result upload retry attempt=%d/%d file=%s",
                    attempt,
                    MAX_ATTEMPTS,
                    file.name,
                )
                if (attempt < MAX_ATTEMPTS) {
                    delay(backoff)
                    backoff *= 2
                }
            }
            return Result.failure(lastFailure ?: IllegalStateException("upload failed"))
        }

        companion object {
            const val MAX_ATTEMPTS: Int = 3
            const val INITIAL_BACKOFF_MS: Long = 2_000L
        }
    }
