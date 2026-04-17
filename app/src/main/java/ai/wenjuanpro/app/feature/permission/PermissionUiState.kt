package ai.wenjuanpro.app.feature.permission

import android.content.Intent

sealed interface PermissionUiState {
    data object Checking : PermissionUiState

    data class NotGranted(
        val intentAvailable: Boolean,
        val hasAttempted: Boolean = false,
    ) : PermissionUiState

    data object Granted : PermissionUiState
}

sealed interface PermissionIntent {
    data object CheckPermission : PermissionIntent

    data object OpenSettings : PermissionIntent

    data object Recheck : PermissionIntent
}

sealed interface PermissionEffect {
    data object NavigateToConfigList : PermissionEffect

    data class LaunchSettings(val intent: Intent) : PermissionEffect

    data object ShowIntentUnavailable : PermissionEffect
}
