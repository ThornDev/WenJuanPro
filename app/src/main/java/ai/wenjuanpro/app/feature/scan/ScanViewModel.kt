package ai.wenjuanpro.app.feature.scan

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.data.permission.CameraPermissionRepository
import ai.wenjuanpro.app.domain.session.SessionStateHolder
import ai.wenjuanpro.app.domain.validation.StudentIdValidator
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScanViewModel
    @Inject
    constructor(
        private val cameraPermissionRepository: CameraPermissionRepository,
        private val sessionStateHolder: SessionStateHolder,
        @ApplicationContext private val context: Context,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val configId: String = savedStateHandle.get<String>(KEY_CONFIG_ID).orEmpty()

        private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle())
        val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ScanEffect>(Channel.BUFFERED)
        val effects: Flow<ScanEffect> = _effects.receiveAsFlow()

        init {
            if (configId.isBlank()) {
                Timber.w("scan entered without configId; code=$CODE_CONFIG_MISSING")
                _effects.trySend(ScanEffect.NavigateBack)
            } else {
                handle(ScanIntent.OnEnter)
            }
        }

        fun onIntent(intent: ScanIntent) {
            if (configId.isBlank()) {
                Timber.d("scan intent ignored; configId missing")
                return
            }
            handle(intent)
        }

        private fun handle(intent: ScanIntent) {
            when (intent) {
                ScanIntent.OnEnter -> checkCameraAndPermission()
                is ScanIntent.OnPermissionResult -> onPermissionResult(intent.granted)
                is ScanIntent.OnQrDecoded -> onQrDecoded(intent.content)
                ScanIntent.OnRetryPermission -> {
                    Timber.d("scan retry permission request")
                    _effects.trySend(ScanEffect.RequestCameraPermission)
                }
                ScanIntent.OnOpenSettings -> {
                    Timber.d("scan open app settings")
                    _effects.trySend(ScanEffect.OpenAppSettings)
                }
                ScanIntent.OnNavigateBack -> {
                    Timber.d("scan navigate back")
                    _effects.trySend(ScanEffect.NavigateBack)
                }
                ScanIntent.OnCameraBindFailed -> {
                    Timber.d("scan bind failed; reason=$REASON_CAMERA_BIND_FAILED")
                    _uiState.value = ScanUiState.PermissionDenied(reason = REASON_CAMERA_BIND_FAILED)
                }
                ScanIntent.SnackbarShown -> clearSnackbar()
            }
        }

        private fun checkCameraAndPermission() {
            if (!cameraPermissionRepository.hasBackCamera(context)) {
                Timber.d("scan state=NoCamera")
                _uiState.value = ScanUiState.NoCamera()
                return
            }
            if (cameraPermissionRepository.isCameraGranted(context)) {
                Timber.d("scan state=Preview")
                _uiState.value = ScanUiState.Preview(configId = configId)
            } else {
                Timber.d("scan state=CheckingPermission; requesting permission")
                _uiState.value = ScanUiState.CheckingPermission()
                _effects.trySend(ScanEffect.RequestCameraPermission)
            }
        }

        private fun onPermissionResult(granted: Boolean) {
            if (granted) {
                Timber.d("scan permission granted; state=Preview")
                _uiState.value = ScanUiState.Preview(configId = configId)
            } else {
                Timber.d("scan reason=$REASON_CAMERA_DENIED")
                _uiState.value = ScanUiState.PermissionDenied(reason = REASON_CAMERA_DENIED)
            }
        }

        private fun onQrDecoded(content: String) {
            val current = _uiState.value
            if (current !is ScanUiState.Preview) {
                Timber.d("scan decode dropped; stateCls=${current::class.simpleName}")
                return
            }
            when (val result = StudentIdValidator.validate(content)) {
                is StudentIdValidator.ValidationResult.Valid -> {
                    val studentId = result.studentId
                    Timber.d("scan recognized; len=${studentId.length} valid")
                    _uiState.value = ScanUiState.Recognized(studentId = studentId)
                    viewModelScope.launch {
                        sessionStateHolder.setStudentId(studentId)
                        _effects.send(
                            ScanEffect.NavigateToWelcome(
                                studentId = studentId,
                                configId = configId,
                            ),
                        )
                    }
                }
                is StudentIdValidator.ValidationResult.Invalid -> {
                    Timber.d(
                        "scan rejected; reason=$REASON_STUDENT_ID_INVALID len=${content.length}",
                    )
                    _uiState.value =
                        current.copy(
                            transientSnackbar = result.reason,
                        )
                }
            }
        }

        private fun clearSnackbar() {
            _uiState.update { state ->
                when (state) {
                    is ScanUiState.Idle -> state.copy(transientSnackbar = null)
                    is ScanUiState.CheckingPermission -> state.copy(transientSnackbar = null)
                    is ScanUiState.Preview -> state.copy(transientSnackbar = null)
                    is ScanUiState.PermissionDenied -> state.copy(transientSnackbar = null)
                    is ScanUiState.NoCamera -> state.copy(transientSnackbar = null)
                    is ScanUiState.Recognized -> state.copy(transientSnackbar = null)
                }
            }
        }

        companion object {
            const val KEY_CONFIG_ID = "configId"

            const val REASON_CAMERA_DENIED = "CAMERA_DENIED"
            const val REASON_CAMERA_BIND_FAILED = "CAMERA_BIND_FAILED"
            const val REASON_STUDENT_ID_INVALID = "STUDENT_ID_INVALID"
            const val CODE_CONFIG_MISSING = "CONFIG_MISSING"
        }
    }
