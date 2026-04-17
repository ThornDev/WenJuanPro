package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.domain.model.AppFailure
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.WriteFailedException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppendResultUseCase
    @Inject
    constructor(
        private val resultRepository: ResultRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend operator fun invoke(record: ResultRecord): Result<Unit> =
            withContext(ioDispatcher) {
                val outcome =
                    runCatching {
                        resultRepository.append(record).getOrThrow()
                    }
                if (outcome.isSuccess) {
                    Result.success(Unit)
                } else {
                    val cause = outcome.exceptionOrNull()
                    Result.failure(
                        AppFailure(
                            code = CODE_RESULT_WRITE_FAILED,
                            cause = cause ?: WriteFailedException(),
                        ),
                    )
                }
            }

        companion object {
            const val CODE_RESULT_WRITE_FAILED: String = "RESULT_WRITE_FAILED"
        }
    }
