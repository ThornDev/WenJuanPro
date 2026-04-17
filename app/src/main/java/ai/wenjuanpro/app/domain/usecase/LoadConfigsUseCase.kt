package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import javax.inject.Inject

class LoadConfigsUseCase
    @Inject
    constructor(
        private val repository: ConfigRepository,
    ) {
        suspend operator fun invoke(): List<ConfigLoadResult> = repository.loadAll()
    }
