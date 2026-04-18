package ai.wenjuanpro.app.feature.question

sealed interface QuestionIntent {
    data object OnEnter : QuestionIntent

    data class SelectOption(val index: Int) : QuestionIntent

    data object Submit : QuestionIntent

    data object TimerExpired : QuestionIntent

    data object StageTransition : QuestionIntent

    data class ToggleOption(val index: Int) : QuestionIntent

    data object Retry : QuestionIntent

    data object FlashComplete : QuestionIntent

    data class RecallTap(val gridIndex: Int) : QuestionIntent

    data object RecallTimeout : QuestionIntent
}
