package ai.wenjuanpro.app.feature.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.ui.components.DotState

sealed interface MemoryPhase {
    data object Rendering : MemoryPhase

    data class Flashing(val currentIndex: Int) : MemoryPhase

    data object Recalling : MemoryPhase
}

sealed interface QuestionUiState {
    val countdownProgress: Float
    val isWarning: Boolean

    data object Loading : QuestionUiState {
        override val countdownProgress: Float = 1f
        override val isWarning: Boolean = false
    }

    data class SingleChoiceAllInOne(
        val qid: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val stem: StemContent,
        val options: List<OptionContent>,
        val selectedIndex: Int? = null,
        val submitEnabled: Boolean = false,
        val showSubmitButton: Boolean = true,
        val optionsPerRow: Int? = null,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class SingleChoiceStaged(
        val qid: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val stem: StemContent,
        val options: List<OptionContent>,
        val stage: Stage,
        val selectedIndex: Int? = null,
        val submitEnabled: Boolean = false,
        val showSubmitButton: Boolean = true,
        val optionsPerRow: Int? = null,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class MultiChoiceAllInOne(
        val qid: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val stem: StemContent,
        val options: List<OptionContent>,
        val selectedIndices: Set<Int> = emptySet(),
        val submitEnabled: Boolean = false,
        val showSubmitButton: Boolean = true,
        val optionsPerRow: Int? = null,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class MultiChoiceStaged(
        val qid: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val stem: StemContent,
        val options: List<OptionContent>,
        val stage: Stage,
        val selectedIndices: Set<Int> = emptySet(),
        val submitEnabled: Boolean = false,
        val showSubmitButton: Boolean = true,
        val optionsPerRow: Int? = null,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class RetryWriteBanner(val retriesLeft: Int) : QuestionUiState {
        override val countdownProgress: Float = 0f
        override val isWarning: Boolean = false
    }

    data class Memory(
        val dotsPositions: List<Int>,
        val dotStates: List<DotState>,
        val phase: MemoryPhase,
        val selectedSequence: List<Int> = emptyList(),
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class FillBlankAllInOne(
        val qid: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val stem: StemContent,
        val answer: String = "",
        val submitEnabled: Boolean = false,
        val showSubmitButton: Boolean = true,
        val numericInput: Boolean = true,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class FillBlankStaged(
        val qid: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val stem: StemContent,
        val stage: Stage,
        val answer: String = "",
        val submitEnabled: Boolean = false,
        val showSubmitButton: Boolean = true,
        val numericInput: Boolean = true,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class IntroDisplay(
        val qid: String,
        val stem: StemContent,
        val showNextButton: Boolean = true,
        val showCountdown: Boolean = true,
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class Error(val message: String) : QuestionUiState {
        override val countdownProgress: Float = 0f
        override val isWarning: Boolean = false
    }

    enum class Stage { STEM, OPTIONS }
}
