package ai.wenjuanpro.app.feature.resume

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.ResultRepositoryImpl
import ai.wenjuanpro.app.data.result.ResumeCandidate
import ai.wenjuanpro.app.domain.model.Config
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

sealed interface ResumeUiState {
    data object Loading : ResumeUiState

    data class Ready(
        val studentId: String,
        val configId: String,
        val title: String,
        val completedCount: Int,
        val totalCount: Int,
        val lastSessionTime: String,
        val showAbandonConfirm: Boolean = false,
        val abandoning: Boolean = false,
    ) : ResumeUiState

    data class Error(val message: String) : ResumeUiState
}

sealed interface ResumeIntent {
    data object OnResume : ResumeIntent
    data object OnAbandonClicked : ResumeIntent
    data object OnAbandonConfirmed : ResumeIntent
    data object OnAbandonCancelled : ResumeIntent
}

sealed interface ResumeEffect {
    data class NavigateToQuestion(val studentId: String, val configId: String) : ResumeEffect
    data class NavigateToWelcome(val studentId: String, val configId: String) : ResumeEffect
}

@HiltViewModel
class ResumeViewModel
    @Inject
    constructor(
        private val configRepository: ConfigRepository,
        private val resultRepository: ResultRepository,
        private val fileSystem: FileSystem,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val studentId: String = savedStateHandle.get<String>(KEY_STUDENT_ID).orEmpty()
        private val configId: String = savedStateHandle.get<String>(KEY_CONFIG_ID).orEmpty()

        private val _uiState = MutableStateFlow<ResumeUiState>(ResumeUiState.Loading)
        val uiState: StateFlow<ResumeUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ResumeEffect>(Channel.BUFFERED)
        val effects: Flow<ResumeEffect> = _effects.receiveAsFlow()

        private var candidate: ResumeCandidate? = null

        init {
            loadResumeData()
        }

        fun onIntent(intent: ResumeIntent) {
            when (intent) {
                ResumeIntent.OnResume -> onResume()
                ResumeIntent.OnAbandonClicked -> onAbandonClicked()
                ResumeIntent.OnAbandonConfirmed -> onAbandonConfirmed()
                ResumeIntent.OnAbandonCancelled -> onAbandonCancelled()
            }
        }

        private fun loadResumeData() {
            viewModelScope.launch {
                val config = resolveConfig(configId)
                if (config == null) {
                    _uiState.value = ResumeUiState.Error(CODE_CONFIG_MISSING)
                    return@launch
                }

                val found = resultRepository.findResumable(studentId, configId)
                if (found == null) {
                    _effects.send(ResumeEffect.NavigateToWelcome(studentId, configId))
                    return@launch
                }

                candidate = found
                val timestamp = extractTimestamp(found.resultFileName)

                _uiState.value = ResumeUiState.Ready(
                    studentId = studentId,
                    configId = configId,
                    title = config.title,
                    completedCount = found.completedQids.size,
                    totalCount = config.questions.size,
                    lastSessionTime = timestamp,
                )
            }
        }

        private fun onResume() {
            viewModelScope.launch {
                _effects.send(ResumeEffect.NavigateToQuestion(studentId, configId))
            }
        }

        private fun onAbandonClicked() {
            val ready = _uiState.value as? ResumeUiState.Ready ?: return
            _uiState.value = ready.copy(showAbandonConfirm = true)
        }

        private fun onAbandonCancelled() {
            val ready = _uiState.value as? ResumeUiState.Ready ?: return
            _uiState.value = ready.copy(showAbandonConfirm = false)
        }

        private fun onAbandonConfirmed() {
            val ready = _uiState.value as? ResumeUiState.Ready ?: return
            val oldCandidate = candidate ?: return
            _uiState.value = ready.copy(showAbandonConfirm = false, abandoning = true)

            viewModelScope.launch {
                val success = withContext(ioDispatcher) {
                    try {
                        val oldPath = ResultRepositoryImpl.RESULTS_DIR + oldCandidate.resultFileName
                        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                            .format(java.util.Date())
                        val newPath = "$oldPath.abandoned.$timestamp"
                        fileSystem.rename(oldPath, newPath)
                        true
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to rename abandoned file")
                        false
                    }
                }

                if (success) {
                    _effects.send(ResumeEffect.NavigateToWelcome(studentId, configId))
                } else {
                    _uiState.value = ResumeUiState.Error(CODE_ABANDON_FAILED)
                }
            }
        }

        private suspend fun resolveConfig(id: String): Config? {
            val results = withContext(ioDispatcher) { configRepository.loadAll() }
            return results
                .asSequence()
                .filterIsInstance<ConfigLoadResult.Valid>()
                .map { it.config }
                .firstOrNull { it.configId == id }
        }

        private fun extractTimestamp(fileName: String): String {
            // Format: {deviceId}_{studentId}_{configId}_{yyyyMMdd-HHmmss}.txt
            val parts = fileName.removeSuffix(".txt").split("_")
            return if (parts.size >= 4) parts.last() else "未知"
        }

        companion object {
            const val KEY_STUDENT_ID = "studentId"
            const val KEY_CONFIG_ID = "configId"
            const val CODE_CONFIG_MISSING = "CONFIG_MISSING"
            const val CODE_ABANDON_FAILED = "ABANDON_RENAME_FAILED"
        }
    }
