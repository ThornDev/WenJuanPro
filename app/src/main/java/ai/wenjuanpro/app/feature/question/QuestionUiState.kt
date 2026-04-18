package ai.wenjuanpro.app.feature.question

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.StemContent

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
        override val countdownProgress: Float = 1f,
        override val isWarning: Boolean = false,
    ) : QuestionUiState

    data class RetryWriteBanner(val retriesLeft: Int) : QuestionUiState {
        override val countdownProgress: Float = 0f
        override val isWarning: Boolean = false
    }

    data class Error(val message: String) : QuestionUiState {
        override val countdownProgress: Float = 0f
        override val isWarning: Boolean = false
    }

    enum class Stage { STEM, OPTIONS }
}
