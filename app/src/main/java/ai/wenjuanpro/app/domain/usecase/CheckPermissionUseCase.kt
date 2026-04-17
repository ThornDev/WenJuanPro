package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.data.permission.PermissionRepository
import javax.inject.Inject

class CheckPermissionUseCase
    @Inject
    constructor(
        private val repository: PermissionRepository,
    ) {
        operator fun invoke(): Boolean = repository.isExternalStorageManager()
    }
