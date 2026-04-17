package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.domain.model.AppFailure
import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.Session
import ai.wenjuanpro.app.domain.model.SsaidUnavailableException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartSessionUseCase
    @Inject
    constructor(
        private val deviceIdProvider: DeviceIdProvider,
        private val resultRepository: ResultRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend operator fun invoke(
            studentId: String,
            config: Config,
            startAt: LocalDateTime,
        ): Result<Session> =
            withContext(ioDispatcher) {
                val deviceId = deviceIdProvider.ssaid()
                if (deviceId.isNullOrBlank()) {
                    return@withContext Result.failure(SsaidUnavailableException())
                }
                val fileName =
                    Session.composeFileName(
                        deviceId = deviceId,
                        studentId = studentId,
                        configId = config.configId,
                        sessionStart = startAt,
                    )
                val session =
                    Session(
                        studentId = studentId,
                        deviceId = deviceId,
                        config = config,
                        sessionStart = startAt,
                        resultFileName = fileName,
                    )
                runCatching {
                    resultRepository.openSession(session)
                    session
                }.recoverCatching { cause ->
                    throw AppFailure(code = "SESSION_OPEN_FAILED", cause = cause)
                }
            }
    }
