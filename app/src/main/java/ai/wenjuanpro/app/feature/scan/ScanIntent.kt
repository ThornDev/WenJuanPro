package ai.wenjuanpro.app.feature.scan

sealed interface ScanIntent {
    data object OnEnter : ScanIntent

    data class OnPermissionResult(val granted: Boolean) : ScanIntent

    data class OnQrDecoded(val content: String) : ScanIntent

    data object OnRetryPermission : ScanIntent

    data object OnOpenSettings : ScanIntent

    data object OnNavigateBack : ScanIntent

    data object OnCameraBindFailed : ScanIntent

    data object SnackbarShown : ScanIntent
}
