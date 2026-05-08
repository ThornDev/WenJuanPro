package ai.wenjuanpro.app.feature.complete

import ai.wenjuanpro.app.domain.session.SessionStateHolder
import ai.wenjuanpro.app.domain.usecase.UploadResultUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CompleteViewModel
    @Inject
    constructor(
        private val sessionStateHolder: SessionStateHolder,
        private val uploadResult: UploadResultUseCase,
    ) : ViewModel() {
        private val _uploadState = MutableStateFlow<CompleteUploadState>(CompleteUploadState.Idle)
        val uploadState: StateFlow<CompleteUploadState> = _uploadState.asStateFlow()

        init {
            triggerUpload()
        }

        private fun triggerUpload() {
            val session = sessionStateHolder.currentSession.value
            val fileName = session?.resultFileName
            if (fileName.isNullOrBlank()) {
                Timber.w("complete upload skipped: no active session/result file")
                _uploadState.value = CompleteUploadState.NoFile
                return
            }
            val file = File(RESULTS_DIR + fileName)
            if (!file.exists()) {
                Timber.w("complete upload skipped: file does not exist %s", file.absolutePath)
                _uploadState.value = CompleteUploadState.NoFile
                return
            }
            viewModelScope.launch {
                val outcome =
                    uploadResult(file) { attempt ->
                        _uploadState.value =
                            CompleteUploadState.Uploading(
                                attempt = attempt,
                                maxAttempts = UploadResultUseCase.MAX_ATTEMPTS,
                            )
                    }
                _uploadState.value =
                    if (outcome.isSuccess) {
                        CompleteUploadState.Success
                    } else {
                        CompleteUploadState.Failed(
                            attempts = UploadResultUseCase.MAX_ATTEMPTS,
                            reason = outcome.exceptionOrNull()?.message,
                        )
                    }
            }
        }

        companion object {
            private const val RESULTS_DIR = "/sdcard/WenJuanPro/results/"
        }
    }
