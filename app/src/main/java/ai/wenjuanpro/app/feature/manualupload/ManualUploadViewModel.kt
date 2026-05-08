package ai.wenjuanpro.app.feature.manualupload

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.domain.usecase.UploadResultUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ManualUploadViewModel
    @Inject
    constructor(
        private val fileSystem: FileSystem,
        private val uploadResult: UploadResultUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ManualUploadUiState(loading = true))
        val state: StateFlow<ManualUploadUiState> = _state.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _state.update { it.copy(loading = true) }
                val files =
                    runCatching {
                        withContext(ioDispatcher) {
                            fileSystem.listFiles(RESULTS_DIR, ".txt")
                        }
                    }.getOrElse {
                        Timber.w(it, "manual upload listFiles failed")
                        emptyList()
                    }
                val rows =
                    files.asSequence()
                        .map { path -> path.substringAfterLast('/') }
                        .sortedDescending()
                        .map { name ->
                            val existing = _state.value.rows.firstOrNull { it.fileName == name }
                            existing ?: FileRow(fileName = name, status = FileStatus.Idle)
                        }
                        .toList()
                _state.update { it.copy(loading = false, rows = rows) }
            }
        }

        fun upload(fileName: String) {
            val file = File(RESULTS_DIR + fileName)
            viewModelScope.launch {
                updateRow(fileName) {
                    it.copy(status = FileStatus.Uploading(attempt = 1, max = UploadResultUseCase.MAX_ATTEMPTS))
                }
                val outcome =
                    uploadResult(file) { attempt ->
                        updateRow(fileName) {
                            it.copy(status = FileStatus.Uploading(attempt, UploadResultUseCase.MAX_ATTEMPTS))
                        }
                    }
                updateRow(fileName) {
                    it.copy(
                        status = if (outcome.isSuccess) FileStatus.Success else FileStatus.Failed,
                    )
                }
            }
        }

        private fun updateRow(
            fileName: String,
            transform: (FileRow) -> FileRow,
        ) {
            _state.update { current ->
                current.copy(
                    rows =
                        current.rows.map { row ->
                            if (row.fileName == fileName) transform(row) else row
                        },
                )
            }
        }

        private fun MutableStateFlow<ManualUploadUiState>.update(
            transform: (ManualUploadUiState) -> ManualUploadUiState,
        ) {
            value = transform(value)
        }

        companion object {
            private const val RESULTS_DIR = "/sdcard/WenJuanPro/results/"
        }
    }

data class ManualUploadUiState(
    val loading: Boolean = false,
    val rows: List<FileRow> = emptyList(),
)

data class FileRow(
    val fileName: String,
    val status: FileStatus,
)

sealed interface FileStatus {
    data object Idle : FileStatus

    data class Uploading(val attempt: Int, val max: Int) : FileStatus

    data object Success : FileStatus

    data object Failed : FileStatus
}
