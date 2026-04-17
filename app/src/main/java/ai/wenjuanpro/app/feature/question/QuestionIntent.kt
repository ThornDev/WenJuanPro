package ai.wenjuanpro.app.feature.question

sealed interface QuestionIntent {
    data object OnEnter : QuestionIntent

    data class SelectOption(val index: Int) : QuestionIntent

    data object Submit : QuestionIntent

    data object TimerExpired : QuestionIntent

    data object StageTransition : QuestionIntent

    data object Retry : QuestionIntent
}
