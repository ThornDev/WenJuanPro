package ai.wenjuanpro.app.feature.welcome

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.core.time.Clock
import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.StartSessionResult
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class WelcomeConfirmViewModel
    @Inject
    constructor(
        private val configRepository: ConfigRepository,
        private val resultRepository: ResultRepository,
        private val deviceIdProvider: DeviceIdProvider,
        private val clock: Clock,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val studentId: String = savedStateHandle.get<String>(KEY_STUDENT_ID).orEmpty()
        private val configId: String = savedStateHandle.get<String>(KEY_CONFIG_ID).orEmpty()

        private val _uiState = MutableStateFlow<WelcomeConfirmUiState>(WelcomeConfirmUiState.Loading)
        val uiState: StateFlow<WelcomeConfirmUiState> = _uiState.asStateFlow()

        private val _effects = Channel<WelcomeConfirmEffect>(Channel.BUFFERED)
        val effects: Flow<WelcomeConfirmEffect> = _effects.receiveAsFlow()

        @Volatile
        private var startInFlight: Boolean = false

        init {
            if (studentId.isBlank() || configId.isBlank()) {
                Timber.w("welcome missing args; code=$CODE_ARGS_MISSING")
                _uiState.value = WelcomeConfirmUiState.ConfigMissing(configId = configId)
            } else {
                loadConfigAndSsaid()
            }
        }

        fun onIntent(intent: WelcomeConfirmIntent) {
            when (intent) {
                WelcomeConfirmIntent.OnEnter -> {
                    if (_uiState.value is WelcomeConfirmUiState.SsaidUnavailable) {
                        loadConfigAndSsaid()
                    }
                }
                WelcomeConfirmIntent.OnRetrySsaid -> loadConfigAndSsaid()
                WelcomeConfirmIntent.OnStartClicked -> onStartClicked()
            }
        }

        private fun loadConfigAndSsaid() {
            _uiState.value = WelcomeConfirmUiState.Loading
            viewModelScope.launch {
                val ssaid =
                    withContext(ioDispatcher) {
                        deviceIdProvider.ssaid()
                    }
                if (ssaid.isNullOrBlank()) {
                    Timber.d("welcome ssaid unavailable; code=$CODE_SSAID_UNAVAILABLE")
                    _uiState.value =
                        WelcomeConfirmUiState.SsaidUnavailable(
                            studentId = studentId,
                            configId = configId,
                        )
                    return@launch
                }

                val config = resolveConfig(configId)
                if (config == null) {
                    Timber.w("welcome config missing; configId=$configId")
                    _uiState.value = WelcomeConfirmUiState.ConfigMissing(configId = configId)
                    return@launch
                }

                val hasResume =
                    resultRepository.hasIncompleteResult(
                        deviceId = ssaid,
                        studentId = studentId,
                        configId = configId,
                        totalQuestions = config.questions.size,
                    )
                if (hasResume) {
                    Timber.d("welcome resume detected; routing to Story 4.1 placeholder")
                    _effects.trySend(
                        WelcomeConfirmEffect.NavigateToResume(
                            studentId = studentId,
                            configId = configId,
                        ),
                    )
                    return@launch
                }

                _uiState.value =
                    WelcomeConfirmUiState.Ready(
                        studentId = studentId,
                        configId = configId,
                        title = config.title,
                        questionCount = config.questions.size,
                        etaMinutes = computeEtaMinutes(config),
                    )
            }
        }

        private suspend fun resolveConfig(id: String): Config? {
            val results =
                withContext(ioDispatcher) {
                    configRepository.loadAll()
                }
            return results
                .asSequence()
                .filterIsInstance<ConfigLoadResult.Valid>()
                .map { it.config }
                .firstOrNull { it.configId == id }
        }

        private fun onStartClicked() {
            val ready = _uiState.value as? WelcomeConfirmUiState.Ready ?: return
            if (startInFlight || ready.starting) {
                Timber.d("start click dedup")
                return
            }
            startInFlight = true
            _uiState.update { (it as? WelcomeConfirmUiState.Ready)?.copy(starting = true) ?: it }

            viewModelScope.launch {
                val ssaid =
                    withContext(ioDispatcher) {
                        deviceIdProvider.ssaid()
                    }
                if (ssaid.isNullOrBlank()) {
                    startInFlight = false
                    _uiState.value =
                        WelcomeConfirmUiState.SsaidUnavailable(
                            studentId = ready.studentId,
                            configId = ready.configId,
                        )
                    return@launch
                }
                val sessionStartMs = clock.nowMs()
                val result =
                    resultRepository.startSession(
                        deviceId = ssaid,
                        studentId = ready.studentId,
                        configId = ready.configId,
                        sessionStartMs = sessionStartMs,
                    )
                when (result) {
                    is StartSessionResult.Success -> {
                        Timber.d("welcome session started fileLen=${result.sessionFileName.length}")
                        _effects.trySend(
                            WelcomeConfirmEffect.NavigateToFirstQuestion(
                                studentId = ready.studentId,
                                configId = ready.configId,
                            ),
                        )
                    }
                    is StartSessionResult.Failure -> {
                        Timber.w("welcome start session failed; code=${result.code}")
                        startInFlight = false
                        _uiState.update {
                            (it as? WelcomeConfirmUiState.Ready)?.copy(starting = false) ?: it
                        }
                    }
                }
            }
        }

        companion object {
            const val KEY_STUDENT_ID = "studentId"
            const val KEY_CONFIG_ID = "configId"
            const val CODE_SSAID_UNAVAILABLE = "SSAID_UNAVAILABLE"
            const val CODE_ARGS_MISSING = "WELCOME_ARGS_MISSING"

            fun computeEtaMinutes(config: Config): Int {
                if (config.totalDurationMs <= 0L) return 0
                return ceil(config.totalDurationMs.toDouble() / 60_000.0).toInt()
            }
        }
    }
