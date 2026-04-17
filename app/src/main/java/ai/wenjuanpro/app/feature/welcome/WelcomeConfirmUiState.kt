package ai.wenjuanpro.app.feature.welcome

sealed interface WelcomeConfirmUiState {
    data object Loading : WelcomeConfirmUiState

    data class Ready(
        val studentId: String,
        val configId: String,
        val title: String,
        val questionCount: Int,
        val etaMinutes: Int,
        val starting: Boolean = false,
    ) : WelcomeConfirmUiState

    data class SsaidUnavailable(val studentId: String, val configId: String) : WelcomeConfirmUiState

    data class ConfigMissing(val configId: String) : WelcomeConfirmUiState
}

sealed interface WelcomeConfirmIntent {
    data object OnEnter : WelcomeConfirmIntent

    data object OnStartClicked : WelcomeConfirmIntent

    data object OnRetrySsaid : WelcomeConfirmIntent
}

sealed interface WelcomeConfirmEffect {
    data class NavigateToFirstQuestion(val studentId: String, val configId: String) : WelcomeConfirmEffect

    data class NavigateToResume(val studentId: String, val configId: String) : WelcomeConfirmEffect
}
