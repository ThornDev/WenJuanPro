package ai.wenjuanpro.app.feature.configlist

import ai.wenjuanpro.app.R
import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.domain.session.SessionStateHolder
import ai.wenjuanpro.app.domain.usecase.LoadConfigsUseCase
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
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigListViewModel
    @Inject
    constructor(
        private val configRepository: ConfigRepository,
        private val loadConfigs: LoadConfigsUseCase,
        private val sessionState: SessionStateHolder,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ConfigListUiState>(ConfigListUiState.Loading)
        val uiState: StateFlow<ConfigListUiState> = _uiState.asStateFlow()

        private val _sheetState = MutableStateFlow<ErrorSheetState?>(null)
        val sheetState: StateFlow<ErrorSheetState?> = _sheetState.asStateFlow()

        private val _effect = Channel<ConfigListEffect>(Channel.UNLIMITED)
        val effect: Flow<ConfigListEffect> = _effect.receiveAsFlow()

        @Volatile
        private var lastSelectedConfigId: String? = null

        @Volatile
        private var refreshInFlight: Boolean = false

        private var currentCards: List<ConfigCardUiModel> = emptyList()

        init {
            onIntent(ConfigListIntent.Refresh)
        }

        fun onIntent(intent: ConfigListIntent) {
            when (intent) {
                ConfigListIntent.Refresh -> refresh()
                is ConfigListIntent.CardClicked -> cardClicked(intent.configId)
                is ConfigListIntent.ViewErrors -> viewErrors(intent.sourceFileName)
                ConfigListIntent.DismissSheet -> _sheetState.value = null
                ConfigListIntent.HiddenAreaLongPressed -> hiddenAreaLongPressed()
            }
        }

        private fun refresh() {
            if (refreshInFlight) {
                Timber.d("refresh ignored: in-flight")
                return
            }
            refreshInFlight = true
            _uiState.value = ConfigListUiState.Loading
            viewModelScope.launch(ioDispatcher) {
                try {
                    configRepository.ensureConfigDir()
                    val results = withTimeoutOrNull(SCAN_TIMEOUT_MS) { loadConfigs() }
                    _uiState.value = mapResults(results)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Timber.e("config scan failed code=${ERROR_CODE_SCAN_FAILED} cls=${e.javaClass.simpleName}")
                    _uiState.value = ConfigListUiState.Error(R.string.config_list_error_title)
                } finally {
                    refreshInFlight = false
                }
            }
        }

        private fun mapResults(results: List<ConfigLoadResult>?): ConfigListUiState {
            if (results == null) {
                Timber.d("config scan timeout code=$ERROR_CODE_SCAN_TIMEOUT")
                return ConfigListUiState.Timeout
            }
            if (results.isEmpty()) {
                currentCards = emptyList()
                return ConfigListUiState.Empty
            }
            val cards = ConfigListUiMapper.toCards(results)
            currentCards = cards
            val allInvalid = cards.all { !it.isValid }
            Timber.d(
                "config scan success configCount=${cards.size} invalidCount=${cards.count { !it.isValid }}",
            )
            return ConfigListUiState.Success(cards = cards, allInvalid = allInvalid)
        }

        private fun cardClicked(configId: String) {
            if (lastSelectedConfigId == configId) {
                Timber.d("cardClicked deduped configId=$configId")
                return
            }
            lastSelectedConfigId = configId
            sessionState.selectConfig(configId)
            _effect.trySend(ConfigListEffect.NavigateToScan(configId))
        }

        private fun viewErrors(sourceFileName: String) {
            val card = currentCards.firstOrNull { it.sourceFileName == sourceFileName }
            if (card == null) {
                Timber.w("viewErrors: no matching card sourceFileName=$sourceFileName")
                return
            }
            _sheetState.value = ErrorSheetState(sourceFileName = sourceFileName, errors = card.errors)
        }

        private fun hiddenAreaLongPressed() {
            Timber.d("hidden entry triggered")
            _effect.trySend(ConfigListEffect.NavigateToDiagnostics)
        }

        companion object {
            const val SCAN_TIMEOUT_MS: Long = 5_000L
            private const val ERROR_CODE_SCAN_TIMEOUT = "SCAN_TIMEOUT"
            private const val ERROR_CODE_SCAN_FAILED = "CONFIG_SCAN_FAILED"
        }
    }
