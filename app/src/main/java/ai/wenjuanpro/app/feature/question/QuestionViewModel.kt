package ai.wenjuanpro.app.feature.question

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.core.time.Clock
import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.domain.fsm.QuestionEvent
import ai.wenjuanpro.app.domain.fsm.QuestionFsm
import ai.wenjuanpro.app.domain.fsm.QuestionFsmState
import ai.wenjuanpro.app.domain.model.AppFailure
import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.Session
import ai.wenjuanpro.app.domain.model.SsaidUnavailableException
import ai.wenjuanpro.app.ui.components.DotState
import ai.wenjuanpro.app.domain.session.SessionStateHolder
import ai.wenjuanpro.app.domain.usecase.AppendResultUseCase
import ai.wenjuanpro.app.domain.usecase.FlashSequenceGenerator
import ai.wenjuanpro.app.domain.usecase.ScoreMemoryUseCase
import ai.wenjuanpro.app.domain.usecase.StartSessionUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel
    @Inject
    constructor(
        private val sessionStateHolder: SessionStateHolder,
        private val configRepository: ConfigRepository,
        private val resultRepository: ResultRepository,
        private val deviceIdProvider: DeviceIdProvider,
        private val startSessionUseCase: StartSessionUseCase,
        private val appendResultUseCase: AppendResultUseCase,
        private val flashSequenceGenerator: FlashSequenceGenerator,
        private val scoreMemoryUseCase: ScoreMemoryUseCase,
        private val questionFsm: QuestionFsm,
        private val clock: Clock,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val studentId: String = savedStateHandle.get<String>(KEY_STUDENT_ID).orEmpty()
        private val configId: String = savedStateHandle.get<String>(KEY_CONFIG_ID).orEmpty()

        private val _uiState = MutableStateFlow<QuestionUiState>(QuestionUiState.Loading)
        val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

        private val _effects = Channel<QuestionEffect>(Channel.BUFFERED)
        val effects: Flow<QuestionEffect> = _effects.receiveAsFlow()

        private var fsmState: QuestionFsmState = QuestionFsmState.Init
        private var config: Config? = null
        private var cursor: Int = 0
        private var countdownJob: Job? = null
        private var flashJob: Job? = null
        private var bootstrapped: Boolean = false

        init {
            if (studentId.isBlank() || configId.isBlank()) {
                Timber.w("question missing args; code=$CODE_ARGS_MISSING")
                _uiState.value = QuestionUiState.Error(CODE_ARGS_MISSING)
            } else {
                onIntent(QuestionIntent.OnEnter)
            }
        }

        fun onIntent(intent: QuestionIntent) {
            when (intent) {
                QuestionIntent.OnEnter -> bootstrapIfNeeded()
                is QuestionIntent.SelectOption -> handleSelect(intent.index)
                is QuestionIntent.ToggleOption -> handleToggle(intent.index)
                QuestionIntent.Submit -> handleSubmit()
                QuestionIntent.TimerExpired -> handleTimerExpired()
                QuestionIntent.StageTransition -> handleStageTransition()
                QuestionIntent.Retry -> handleRetry()
                QuestionIntent.FlashComplete -> { /* handled by flash coroutine */ }
                is QuestionIntent.RecallTap -> handleRecallTap(intent.gridIndex)
                QuestionIntent.RecallTimeout -> handleRecallTimeout()
                is QuestionIntent.UpdateFillBlank -> handleFillBlankUpdate(intent.answer)
            }
        }

        private fun handleFillBlankUpdate(answer: String) {
            val current = fsmState
            if (current !is QuestionFsmState.FillBlankAllInOne &&
                current !is QuestionFsmState.FillBlankStagedOptions
            ) {
                return
            }
            fsmState = questionFsm.reduce(
                current, QuestionEvent.UpdateFillBlankAnswer(answer), clock.nowMs(),
            )
            renderUiFromFsm(clock.nowMs())
        }

        private fun bootstrapIfNeeded() {
            if (bootstrapped) return
            bootstrapped = true
            _uiState.value = QuestionUiState.Loading
            viewModelScope.launch {
                val loaded = loadConfig()
                if (loaded == null) {
                    Timber.w("question config not found; configId=$configId")
                    _uiState.value = QuestionUiState.Error(CODE_CONFIG_MISSING)
                    return@launch
                }
                config = loaded
                sessionStateHolder.setSelectedConfig(loaded)

                val resumed = tryResume(loaded)
                val session =
                    if (resumed != null) {
                        Timber.d(
                            "question resumed cursor=${resumed.cursor} completed=${resumed.completedQids.size}",
                        )
                        resumed
                    } else {
                        val sessionResult =
                            startSessionUseCase(
                                studentId = studentId,
                                config = loaded,
                                startAt = nowLocalDateTime(),
                            )
                        sessionResult.getOrElse { failure ->
                            val message =
                                if (failure is SsaidUnavailableException) {
                                    CODE_SSAID_UNAVAILABLE
                                } else {
                                    (failure as? AppFailure)?.code ?: CODE_SESSION_OPEN_FAILED
                                }
                            Timber.w("question startSession failed; code=$message")
                            _uiState.value = QuestionUiState.Error(message)
                            return@launch
                        }
                    }
                sessionStateHolder.openSession(session)
                cursor = session.cursor

                enterCurrentQuestion()
            }
        }

        private suspend fun tryResume(loaded: Config): Session? {
            val candidate = runCatching {
                resultRepository.findResumable(studentId, loaded.configId)
            }.getOrNull() ?: return null
            if (candidate.completedQids.isEmpty()) return null
            if (candidate.completedQids.size >= loaded.questions.size) return null
            val deviceId =
                withContext(ioDispatcher) { deviceIdProvider.ssaid() }
                    ?.takeIf { it.isNotBlank() }
                    ?: return null
            val session = Session(
                studentId = studentId,
                deviceId = deviceId,
                config = loaded,
                sessionStart = nowLocalDateTime(),
                resultFileName = candidate.resultFileName,
                cursor = candidate.completedQids.size,
                completedQids = candidate.completedQids,
            )
            return runCatching {
                resultRepository.openSession(session)
                session
            }.onFailure {
                Timber.w(it, "resume openSession failed; falling back to fresh start")
            }.getOrNull()
        }

        private suspend fun loadConfig(): Config? {
            sessionStateHolder.selectedConfig.value?.takeIf { it.configId == configId }?.let {
                return it
            }
            val all =
                withContext(ioDispatcher) {
                    configRepository.loadAll()
                }
            return all.asSequence()
                .filterIsInstance<ConfigLoadResult.Valid>()
                .map { it.config }
                .firstOrNull { it.configId == configId }
        }

        private fun enterCurrentQuestion() {
            val cfg = config ?: return
            if (cursor >= cfg.questions.size) {
                viewModelScope.launch { _effects.send(QuestionEffect.NavigateComplete) }
                return
            }
            val question = cfg.questions[cursor]
            val now = clock.nowMs()
            fsmState =
                when (question) {
                    is Question.SingleChoice ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(question), now)
                    is Question.MultiChoice ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(question), now)
                    is Question.Memory ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(question), now)
                    is Question.FillBlank ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterFillBlank(question), now)
                }
            renderUiFromFsm(now)
            if (fsmState is QuestionFsmState.MemoryRendering) {
                startFlashSequenceAfterDelay(question as Question.Memory)
            } else {
                startCountdownForCurrentStage()
            }
        }

        private fun handleSelect(index: Int) {
            val current = fsmState
            if (current !is QuestionFsmState.QuestionAllInOne &&
                current !is QuestionFsmState.QuestionStagedOptions
            ) {
                return
            }
            fsmState =
                questionFsm.reduce(current, QuestionEvent.SelectOption(index), clock.nowMs())
            renderUiFromFsm(clock.nowMs())
            val autoAdvance =
                when (val updated = fsmState) {
                    is QuestionFsmState.QuestionAllInOne -> updated.question.autoAdvance
                    is QuestionFsmState.QuestionStagedOptions -> updated.question.autoAdvance
                    else -> false
                }
            if (autoAdvance) handleSubmit()
        }

        private fun handleToggle(index: Int) {
            val current = fsmState
            if (current !is QuestionFsmState.MultiAllInOne &&
                current !is QuestionFsmState.MultiStagedOptions
            ) {
                return
            }
            fsmState =
                questionFsm.reduce(current, QuestionEvent.ToggleOption(index), clock.nowMs())
            renderUiFromFsm(clock.nowMs())
        }

        private fun handleSubmit() {
            val current = fsmState
            when (current) {
                is QuestionFsmState.QuestionAllInOne -> {
                    val selected = current.selectedIndex ?: return
                    cancelCountdown()
                    val now = clock.nowMs()
                    fsmState = questionFsm.reduce(current, QuestionEvent.OptionsSubmit(selected), now)
                    renderUiFromFsm(now)
                    persistWriting()
                }
                is QuestionFsmState.QuestionStagedOptions -> {
                    val selected = current.selectedIndex ?: return
                    cancelCountdown()
                    val now = clock.nowMs()
                    fsmState = questionFsm.reduce(current, QuestionEvent.OptionsSubmit(selected), now)
                    renderUiFromFsm(now)
                    persistWriting()
                }
                is QuestionFsmState.MultiAllInOne -> {
                    if (current.selectedIndices.isEmpty()) return
                    cancelCountdown()
                    val now = clock.nowMs()
                    fsmState = questionFsm.reduce(current, QuestionEvent.MultiOptionsSubmit(current.selectedIndices), now)
                    renderUiFromFsm(now)
                    persistWriting()
                }
                is QuestionFsmState.MultiStagedOptions -> {
                    if (current.selectedIndices.isEmpty()) return
                    cancelCountdown()
                    val now = clock.nowMs()
                    fsmState = questionFsm.reduce(current, QuestionEvent.MultiOptionsSubmit(current.selectedIndices), now)
                    renderUiFromFsm(now)
                    persistWriting()
                }
                is QuestionFsmState.FillBlankAllInOne -> {
                    if (current.answer.isBlank()) return
                    cancelCountdown()
                    val now = clock.nowMs()
                    fsmState = questionFsm.reduce(current, QuestionEvent.FillBlankSubmit(current.answer), now)
                    renderUiFromFsm(now)
                    persistWriting()
                }
                is QuestionFsmState.FillBlankStagedOptions -> {
                    if (current.answer.isBlank()) return
                    cancelCountdown()
                    val now = clock.nowMs()
                    fsmState = questionFsm.reduce(current, QuestionEvent.FillBlankSubmit(current.answer), now)
                    renderUiFromFsm(now)
                    persistWriting()
                }
                else -> return
            }
        }

        private fun handleTimerExpired() {
            val current = fsmState
            when (current) {
                is QuestionFsmState.QuestionAllInOne,
                is QuestionFsmState.QuestionStagedOptions,
                is QuestionFsmState.MultiAllInOne,
                is QuestionFsmState.MultiStagedOptions,
                is QuestionFsmState.FillBlankAllInOne,
                is QuestionFsmState.FillBlankStagedOptions,
                -> {
                    cancelCountdown()
                    fsmState =
                        questionFsm.reduce(current, QuestionEvent.OptionsTimeout, clock.nowMs())
                    persistWriting()
                }
                is QuestionFsmState.QuestionStagedStem,
                is QuestionFsmState.MultiStagedStem,
                is QuestionFsmState.FillBlankStagedStem,
                -> {
                    cancelCountdown()
                    fsmState = questionFsm.reduce(current, QuestionEvent.StemTimeout, clock.nowMs())
                    renderUiFromFsm(clock.nowMs())
                    startCountdownForCurrentStage()
                }
                is QuestionFsmState.MemoryRecalling -> {
                    handleRecallTimeout()
                }
                else -> Unit
            }
        }

        private fun handleStageTransition() {
            val current = fsmState
            if (current !is QuestionFsmState.QuestionStagedStem &&
                current !is QuestionFsmState.MultiStagedStem &&
                current !is QuestionFsmState.FillBlankStagedStem
            ) {
                return
            }
            cancelCountdown()
            fsmState = questionFsm.reduce(current, QuestionEvent.StemTimeout, clock.nowMs())
            renderUiFromFsm(clock.nowMs())
            startCountdownForCurrentStage()
        }

        private fun handleRecallTap(gridIndex: Int) {
            val current = fsmState
            if (current !is QuestionFsmState.MemoryRecalling) return
            fsmState = questionFsm.reduce(current, QuestionEvent.RecallTap(gridIndex), clock.nowMs())
            renderUiFromFsm(clock.nowMs())
            val updated = fsmState as? QuestionFsmState.MemoryRecalling ?: return
            if (updated.selectedSequence.size >= current.question.dotsPositions.size) {
                cancelCountdown()
                finishMemoryRecall(updated)
            }
        }

        private fun handleRecallTimeout() {
            val current = fsmState
            if (current !is QuestionFsmState.MemoryRecalling) return
            cancelCountdown()
            finishMemoryRecall(current)
        }

        private fun finishMemoryRecall(state: QuestionFsmState.MemoryRecalling) {
            val optionsMs = clock.nowMs() - state.stageEnteredMs
            val record = scoreMemoryUseCase(
                question = state.question,
                answer = state.selectedSequence,
                expected = state.flashSequence,
                optionsMs = optionsMs,
            )
            fsmState = QuestionFsmState.Writing(record = record, retriesLeft = 3)
            persistWriting()
        }

        private fun startFlashSequenceAfterDelay(question: Question.Memory) {
            flashJob?.cancel()
            flashJob = viewModelScope.launch {
                try {
                    delay(FLASH_INITIAL_DELAY_MS)
                    val flashSequence = flashSequenceGenerator.generate(question.dotsPositions)
                    fsmState = questionFsm.reduce(
                        fsmState, QuestionEvent.FlashStart(flashSequence), clock.nowMs(),
                    )
                    renderUiFromFsm(clock.nowMs())

                    for (i in flashSequence.indices) {
                        if (i > 0) {
                            fsmState = questionFsm.reduce(
                                fsmState, QuestionEvent.FlashTick(i), clock.nowMs(),
                            )
                        }
                        renderUiFromFsm(clock.nowMs())
                        delay(question.flashDurationMs)
                        renderUiFromFsm(clock.nowMs())
                        if (i < flashSequence.size - 1) {
                            delay(question.flashIntervalMs)
                        }
                    }

                    fsmState = questionFsm.reduce(
                        fsmState, QuestionEvent.FlashComplete, clock.nowMs(),
                    )
                    // Set stageEnteredMs for countdown in Recalling phase
                    val recalling = fsmState as? QuestionFsmState.MemoryRecalling
                    if (recalling != null) {
                        fsmState = recalling.copy(stageEnteredMs = clock.nowMs())
                    }
                    renderUiFromFsm(clock.nowMs())
                    startCountdownForCurrentStage()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Timber.w("flash sequence failed; qid=${question.qid} error=${e.message}")
                    _uiState.value = QuestionUiState.Error(CODE_ANIMATION_FAILED)
                }
            }
        }

        private fun handleRetry() {
            val current = fsmState
            if (current !is QuestionFsmState.WriteError) return
            fsmState = questionFsm.reduce(current, QuestionEvent.Retry, clock.nowMs())
            persistWriting()
        }

        private fun persistWriting() {
            val writing = fsmState as? QuestionFsmState.Writing ?: return
            val record = writing.record
            viewModelScope.launch {
                val outcome = appendResultUseCase(record)
                if (outcome.isSuccess) {
                    fsmState =
                        questionFsm.reduce(fsmState, QuestionEvent.WriteSuccess, clock.nowMs())
                    advanceAfterWriteSuccess(record.qid)
                } else {
                    fsmState =
                        questionFsm.reduce(fsmState, QuestionEvent.WriteFailure, clock.nowMs())
                    val errored = fsmState as? QuestionFsmState.WriteError
                    val retriesLeft = errored?.retriesLeft ?: 0
                    Timber.w(
                        "question write failed; qid=${record.qid} status=${record.status} retriesLeft=$retriesLeft",
                    )
                    if (retriesLeft <= 0) {
                        fsmState =
                            questionFsm.reduce(fsmState, QuestionEvent.RetryExhausted, clock.nowMs())
                        _effects.send(QuestionEffect.NavigateTerminal)
                    } else {
                        showRetryBanner(retriesLeft)
                        _effects.send(QuestionEffect.ShowRetryBanner(retriesLeft))
                    }
                }
            }
        }

        private suspend fun advanceAfterWriteSuccess(qid: String) {
            val cfg = config ?: return
            sessionStateHolder.advanceCursor(qid)
            cursor += 1
            if (cursor >= cfg.questions.size) {
                Timber.d(
                    "question completed; configId=$configId completedQids=${sessionStateHolder.currentSession.value?.completedQids?.size}",
                )
                _effects.send(QuestionEffect.NavigateComplete)
                return
            }
            val nextQuestion = cfg.questions[cursor]
            val now = clock.nowMs()
            fsmState =
                when (nextQuestion) {
                    is Question.SingleChoice ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(nextQuestion), now)
                    is Question.MultiChoice ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(nextQuestion), now)
                    is Question.Memory ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(nextQuestion), now)
                    is Question.FillBlank ->
                        questionFsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterFillBlank(nextQuestion), now)
                }
            renderUiFromFsm(now)
            if (fsmState is QuestionFsmState.MemoryRendering) {
                startFlashSequenceAfterDelay(nextQuestion as Question.Memory)
            } else {
                startCountdownForCurrentStage()
            }
            _effects.send(
                QuestionEffect.NavigateNext(
                    nextQid = nextQuestion.qid,
                    studentId = studentId,
                    configId = configId,
                ),
            )
        }

        private fun showRetryBanner(retriesLeft: Int) {
            _uiState.value = QuestionUiState.RetryWriteBanner(retriesLeft = retriesLeft)
        }

        private fun renderUiFromFsm(nowMs: Long) {
            val cfg = config
            val totalQuestions = cfg?.questions?.size ?: 0
            val state = fsmState
            _uiState.value =
                when (state) {
                    is QuestionFsmState.Init -> QuestionUiState.Loading
                    is QuestionFsmState.QuestionAllInOne -> {
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.SingleChoiceAllInOne(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            options = state.question.options,
                            selectedIndex = state.selectedIndex,
                            submitEnabled = state.selectedIndex != null,
                            showSubmitButton = state.question.showSubmitButton,
                            optionsPerRow = state.question.optionsPerRow,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.QuestionStagedStem -> {
                        val durationMs =
                            (state.question.stemDurationMs ?: 1L).coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.SingleChoiceStaged(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            options = state.question.options,
                            stage = QuestionUiState.Stage.STEM,
                            selectedIndex = null,
                            submitEnabled = false,
                            showSubmitButton = state.question.showSubmitButton,
                            optionsPerRow = state.question.optionsPerRow,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.QuestionStagedOptions -> {
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.SingleChoiceStaged(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            options = state.question.options,
                            stage = QuestionUiState.Stage.OPTIONS,
                            selectedIndex = state.selectedIndex,
                            submitEnabled = state.selectedIndex != null,
                            showSubmitButton = state.question.showSubmitButton,
                            optionsPerRow = state.question.optionsPerRow,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.MultiAllInOne -> {
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.MultiChoiceAllInOne(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            options = state.question.options,
                            selectedIndices = state.selectedIndices,
                            submitEnabled = state.selectedIndices.isNotEmpty(),
                            showSubmitButton = state.question.showSubmitButton,
                            optionsPerRow = state.question.optionsPerRow,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.MultiStagedStem -> {
                        val durationMs =
                            (state.question.stemDurationMs ?: 1L).coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.MultiChoiceStaged(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            options = state.question.options,
                            stage = QuestionUiState.Stage.STEM,
                            selectedIndices = emptySet(),
                            submitEnabled = false,
                            showSubmitButton = state.question.showSubmitButton,
                            optionsPerRow = state.question.optionsPerRow,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.MultiStagedOptions -> {
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.MultiChoiceStaged(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            options = state.question.options,
                            stage = QuestionUiState.Stage.OPTIONS,
                            selectedIndices = state.selectedIndices,
                            submitEnabled = state.selectedIndices.isNotEmpty(),
                            showSubmitButton = state.question.showSubmitButton,
                            optionsPerRow = state.question.optionsPerRow,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.MemoryRendering -> {
                        val positions = state.question.dotsPositions
                        val dotStates = List(64) { index ->
                            if (index in positions) DotState.BLUE else DotState.EMPTY
                        }
                        QuestionUiState.Memory(
                            dotsPositions = positions,
                            dotStates = dotStates,
                            phase = MemoryPhase.Rendering,
                            countdownProgress = 1f,
                            isWarning = false,
                        )
                    }
                    is QuestionFsmState.MemoryFlashing -> {
                        val positions = state.question.dotsPositions
                        val flashingDotIndex = state.flashSequence[state.currentFlashIndex]
                        val dotStates = List(64) { index ->
                            when {
                                index == flashingDotIndex -> DotState.FLASHING
                                index in positions -> DotState.BLUE
                                else -> DotState.EMPTY
                            }
                        }
                        QuestionUiState.Memory(
                            dotsPositions = positions,
                            dotStates = dotStates,
                            phase = MemoryPhase.Flashing(state.currentFlashIndex),
                            countdownProgress = 1f,
                            isWarning = false,
                        )
                    }
                    is QuestionFsmState.MemoryRecalling -> {
                        val positions = state.question.dotsPositions
                        val selectedSet = state.selectedSequence.toSet()
                        val dotStates = List(64) { index ->
                            when {
                                index in selectedSet -> DotState.SELECTED
                                index in positions -> DotState.BLUE
                                else -> DotState.EMPTY
                            }
                        }
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = if (state.stageEnteredMs > 0L) {
                            (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        } else {
                            0L
                        }
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress = (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.Memory(
                            dotsPositions = positions,
                            dotStates = dotStates,
                            phase = MemoryPhase.Recalling,
                            selectedSequence = state.selectedSequence,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.FillBlankAllInOne -> {
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.FillBlankAllInOne(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            answer = state.answer,
                            submitEnabled = state.answer.isNotBlank(),
                            showSubmitButton = state.question.showSubmitButton,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.FillBlankStagedStem -> {
                        val durationMs =
                            (state.question.stemDurationMs ?: 1L).coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.FillBlankStaged(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            stage = QuestionUiState.Stage.STEM,
                            answer = "",
                            submitEnabled = false,
                            showSubmitButton = state.question.showSubmitButton,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.FillBlankStagedOptions -> {
                        val durationMs = state.question.optionsDurationMs.coerceAtLeast(1L)
                        val elapsed = (nowMs - state.stageEnteredMs).coerceAtLeast(0L)
                        val remaining = (durationMs - elapsed).coerceAtLeast(0L)
                        val progress =
                            (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        QuestionUiState.FillBlankStaged(
                            qid = state.question.qid,
                            questionIndex = cursor + 1,
                            totalQuestions = totalQuestions,
                            stem = state.question.stem,
                            stage = QuestionUiState.Stage.OPTIONS,
                            answer = state.answer,
                            submitEnabled = state.answer.isNotBlank(),
                            showSubmitButton = state.question.showSubmitButton,
                            countdownProgress = progress,
                            isWarning = remaining <= WARNING_THRESHOLD_MS,
                        )
                    }
                    is QuestionFsmState.Writing -> _uiState.value
                    is QuestionFsmState.WriteError -> _uiState.value
                    is QuestionFsmState.NextDecision -> _uiState.value
                    QuestionFsmState.Terminated -> _uiState.value
                    is QuestionFsmState.Errored -> QuestionUiState.Error(state.message)
                }
        }

        private fun startCountdownForCurrentStage() {
            cancelCountdown()
            val (stageStart, durationMs) =
                when (val s = fsmState) {
                    is QuestionFsmState.QuestionAllInOne ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    is QuestionFsmState.QuestionStagedStem ->
                        s.stageEnteredMs to (s.question.stemDurationMs ?: 0L)
                    is QuestionFsmState.QuestionStagedOptions ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    is QuestionFsmState.MultiAllInOne ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    is QuestionFsmState.MultiStagedStem ->
                        s.stageEnteredMs to (s.question.stemDurationMs ?: 0L)
                    is QuestionFsmState.MultiStagedOptions ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    is QuestionFsmState.MemoryRecalling ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    is QuestionFsmState.FillBlankAllInOne ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    is QuestionFsmState.FillBlankStagedStem ->
                        s.stageEnteredMs to (s.question.stemDurationMs ?: 0L)
                    is QuestionFsmState.FillBlankStagedOptions ->
                        s.stageEnteredMs to s.question.optionsDurationMs
                    else -> return
                }
            if (durationMs <= 0L) {
                onIntent(QuestionIntent.TimerExpired)
                return
            }
            countdownJob =
                viewModelScope.launch {
                    while (isActive) {
                        val now = clock.nowMs()
                        val elapsed = now - stageStart
                        if (elapsed >= durationMs) {
                            onIntent(QuestionIntent.TimerExpired)
                            break
                        }
                        renderUiFromFsm(now)
                        delay(COUNTDOWN_TICK_MS)
                    }
                }
        }

        private fun cancelCountdown() {
            countdownJob?.cancel()
            countdownJob = null
        }

        private fun nowLocalDateTime(): LocalDateTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowMs()), ZoneId.systemDefault())

        override fun onCleared() {
            cancelCountdown()
            flashJob?.cancel()
            super.onCleared()
        }

        companion object {
            const val KEY_STUDENT_ID: String = "studentId"
            const val KEY_CONFIG_ID: String = "configId"
            const val CODE_ARGS_MISSING: String = "QUESTION_ARGS_MISSING"
            const val CODE_SSAID_UNAVAILABLE: String = "SSAID_UNAVAILABLE"
            const val CODE_CONFIG_MISSING: String = "CONFIG_MISSING"
            const val CODE_SESSION_OPEN_FAILED: String = "SESSION_OPEN_FAILED"
            const val CODE_UNSUPPORTED_QUESTION: String = "QUESTION_TYPE_UNSUPPORTED"
            const val WARNING_THRESHOLD_MS: Long = 5_000L
            const val COUNTDOWN_TICK_MS: Long = 100L
            const val FLASH_INITIAL_DELAY_MS: Long = 500L
            const val CODE_ANIMATION_FAILED: String = "ANIMATION_FAILED"
        }
    }
