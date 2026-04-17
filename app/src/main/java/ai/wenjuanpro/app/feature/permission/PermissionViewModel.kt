package ai.wenjuanpro.app.feature.permission

import ai.wenjuanpro.app.data.permission.PermissionRepository
import ai.wenjuanpro.app.domain.usecase.CheckPermissionUseCase
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel
    @Inject
    constructor(
        private val checkPermission: CheckPermissionUseCase,
        private val repository: PermissionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PermissionUiState>(PermissionUiState.Checking)
        val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

        private val _effect = Channel<PermissionEffect>(Channel.UNLIMITED)
        val effect: Flow<PermissionEffect> = _effect.receiveAsFlow()

        fun onIntent(intent: PermissionIntent) {
            when (intent) {
                PermissionIntent.CheckPermission, PermissionIntent.Recheck -> refresh()
                PermissionIntent.OpenSettings -> openSettings()
            }
        }

        private fun refresh() {
            val granted = checkPermission()
            if (granted) {
                _uiState.value = PermissionUiState.Granted
                Timber.d("permission state changed granted=true")
                _effect.trySend(PermissionEffect.NavigateToConfigList)
                return
            }
            val intentAvailable = repository.buildManageStorageIntent() != null
            val previousAttempted =
                (_uiState.value as? PermissionUiState.NotGranted)?.hasAttempted ?: false
            _uiState.value =
                PermissionUiState.NotGranted(
                    intentAvailable = intentAvailable,
                    hasAttempted = previousAttempted,
                )
            Timber.d(
                "permission state changed granted=false intentAvailable=$intentAvailable",
            )
        }

        private fun openSettings() {
            val intent = repository.buildManageStorageIntent()
            if (intent == null) {
                _uiState.update {
                    if (it is PermissionUiState.NotGranted) {
                        it.copy(intentAvailable = false)
                    } else {
                        it
                    }
                }
                Timber.d("permission openSettings intentAvailable=false")
                _effect.trySend(PermissionEffect.ShowIntentUnavailable)
                return
            }
            _uiState.update {
                if (it is PermissionUiState.NotGranted) {
                    it.copy(hasAttempted = true)
                } else {
                    it
                }
            }
            Timber.d("permission openSettings intentAvailable=true hasAttempted=true")
            _effect.trySend(PermissionEffect.LaunchSettings(intent))
        }
    }
