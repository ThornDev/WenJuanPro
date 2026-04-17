package ai.wenjuanpro.app.domain.fsm

import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.usecase.ScoreSingleChoiceUseCase
import javax.inject.Inject
import javax.inject.Singleton

sealed interface QuestionFsmState {
    data object Init : QuestionFsmState

    data class QuestionAllInOne(
        val question: Question.SingleChoice,
        val stageEnteredMs: Long,
        val selectedIndex: Int? = null,
    ) : QuestionFsmState

    data class QuestionStagedStem(
        val question: Question.SingleChoice,
        val stageEnteredMs: Long,
    ) : QuestionFsmState

    data class QuestionStagedOptions(
        val question: Question.SingleChoice,
        val stemMs: Long,
        val stageEnteredMs: Long,
        val selectedIndex: Int? = null,
    ) : QuestionFsmState

    data class Writing(
        val record: ResultRecord,
        val retriesLeft: Int = 3,
    ) : QuestionFsmState

    data class WriteError(
        val record: ResultRecord,
        val retriesLeft: Int,
    ) : QuestionFsmState

    data class NextDecision(val lastQid: String) : QuestionFsmState

    data object Terminated : QuestionFsmState

    data class Errored(val message: String) : QuestionFsmState
}

sealed interface QuestionEvent {
    data class Enter(val question: Question.SingleChoice) : QuestionEvent

    data object StemTimeout : QuestionEvent

    data class OptionsSubmit(val index: Int) : QuestionEvent

    data object OptionsTimeout : QuestionEvent

    data class SelectOption(val index: Int) : QuestionEvent

    data object WriteSuccess : QuestionEvent

    data object WriteFailure : QuestionEvent

    data object Retry : QuestionEvent

    data object RetryExhausted : QuestionEvent
}

@Singleton
class QuestionFsm
    @Inject
    constructor(
        private val score: ScoreSingleChoiceUseCase,
    ) {
        fun reduce(
            state: QuestionFsmState,
            event: QuestionEvent,
            nowMs: Long,
        ): QuestionFsmState =
            when (event) {
                is QuestionEvent.Enter -> onEnter(event.question, nowMs)
                is QuestionEvent.SelectOption -> onSelectOption(state, event.index)
                is QuestionEvent.OptionsSubmit -> onOptionsSubmit(state, event.index, nowMs)
                QuestionEvent.OptionsTimeout -> onOptionsTimeout(state)
                QuestionEvent.StemTimeout -> onStemTimeout(state, nowMs)
                QuestionEvent.WriteSuccess -> onWriteSuccess(state)
                QuestionEvent.WriteFailure -> onWriteFailure(state)
                QuestionEvent.Retry -> onRetry(state)
                QuestionEvent.RetryExhausted -> onRetryExhausted(state)
            }

        private fun onEnter(
            question: Question.SingleChoice,
            nowMs: Long,
        ): QuestionFsmState =
            when (question.mode) {
                PresentMode.ALL_IN_ONE ->
                    QuestionFsmState.QuestionAllInOne(
                        question = question,
                        stageEnteredMs = nowMs,
                    )
                PresentMode.STAGED -> {
                    val stemMs = question.stemDurationMs
                    if (stemMs == null) {
                        QuestionFsmState.Errored(
                            "staged question missing stemDurationMs; qid=${question.qid}",
                        )
                    } else {
                        QuestionFsmState.QuestionStagedStem(
                            question = question,
                            stageEnteredMs = nowMs,
                        )
                    }
                }
            }

        private fun onSelectOption(
            state: QuestionFsmState,
            index: Int,
        ): QuestionFsmState =
            when (state) {
                is QuestionFsmState.QuestionAllInOne -> state.copy(selectedIndex = index)
                is QuestionFsmState.QuestionStagedOptions -> state.copy(selectedIndex = index)
                else -> state
            }

        private fun onOptionsSubmit(
            state: QuestionFsmState,
            index: Int,
            nowMs: Long,
        ): QuestionFsmState =
            when (state) {
                is QuestionFsmState.QuestionAllInOne -> {
                    val optionsMs = nowMs - state.stageEnteredMs
                    writingFromScore(
                        question = state.question,
                        answer = index,
                        stemMs = null,
                        optionsMs = optionsMs,
                    )
                }
                is QuestionFsmState.QuestionStagedOptions -> {
                    val optionsMs = nowMs - state.stageEnteredMs
                    writingFromScore(
                        question = state.question,
                        answer = index,
                        stemMs = state.stemMs,
                        optionsMs = optionsMs,
                    )
                }
                else -> state
            }

        private fun onOptionsTimeout(state: QuestionFsmState): QuestionFsmState =
            when (state) {
                is QuestionFsmState.QuestionAllInOne ->
                    writingFromScore(
                        question = state.question,
                        answer = null,
                        stemMs = null,
                        optionsMs = state.question.optionsDurationMs,
                    )
                is QuestionFsmState.QuestionStagedOptions ->
                    writingFromScore(
                        question = state.question,
                        answer = null,
                        stemMs = state.stemMs,
                        optionsMs = state.question.optionsDurationMs,
                    )
                else -> state
            }

        private fun onStemTimeout(
            state: QuestionFsmState,
            nowMs: Long,
        ): QuestionFsmState =
            when (state) {
                is QuestionFsmState.QuestionStagedStem -> {
                    val stemMs = state.question.stemDurationMs
                    if (stemMs == null) {
                        QuestionFsmState.Errored("stem timeout but stemDurationMs null")
                    } else {
                        QuestionFsmState.QuestionStagedOptions(
                            question = state.question,
                            stemMs = stemMs,
                            stageEnteredMs = nowMs,
                        )
                    }
                }
                else -> state
            }

        private fun onWriteSuccess(state: QuestionFsmState): QuestionFsmState =
            when (state) {
                is QuestionFsmState.Writing ->
                    QuestionFsmState.NextDecision(lastQid = state.record.qid)
                else -> state
            }

        private fun onWriteFailure(state: QuestionFsmState): QuestionFsmState =
            when (state) {
                is QuestionFsmState.Writing -> {
                    val next = state.retriesLeft - 1
                    QuestionFsmState.WriteError(
                        record = state.record,
                        retriesLeft = next.coerceAtLeast(0),
                    )
                }
                else -> state
            }

        private fun onRetry(state: QuestionFsmState): QuestionFsmState =
            when (state) {
                is QuestionFsmState.WriteError ->
                    QuestionFsmState.Writing(
                        record = state.record,
                        retriesLeft = state.retriesLeft,
                    )
                else -> state
            }

        private fun onRetryExhausted(state: QuestionFsmState): QuestionFsmState =
            when (state) {
                is QuestionFsmState.WriteError ->
                    if (state.retriesLeft <= 0) QuestionFsmState.Terminated else state
                else -> state
            }

        private fun writingFromScore(
            question: Question.SingleChoice,
            answer: Int?,
            stemMs: Long?,
            optionsMs: Long,
        ): QuestionFsmState =
            try {
                val record: ResultRecord =
                    score(
                        question = question,
                        answer = answer,
                        stemMs = stemMs,
                        optionsMs = optionsMs,
                    )
                QuestionFsmState.Writing(record = record, retriesLeft = 3)
            } catch (e: IllegalStateException) {
                QuestionFsmState.Errored(message = e.message ?: "scoring failure")
            }
    }
