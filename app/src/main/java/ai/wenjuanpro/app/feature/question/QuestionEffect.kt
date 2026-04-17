package ai.wenjuanpro.app.feature.question

sealed interface QuestionEffect {
    data class NavigateNext(
        val nextQid: String,
        val studentId: String,
        val configId: String,
    ) : QuestionEffect

    data object NavigateComplete : QuestionEffect

    data class ShowRetryBanner(val retriesLeft: Int) : QuestionEffect

    data object NavigateTerminal : QuestionEffect
}
