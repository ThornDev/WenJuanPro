package ai.wenjuanpro.app.feature.scan

sealed interface ScanUiState {
    val transientSnackbar: String?

    data class Idle(override val transientSnackbar: String? = null) : ScanUiState

    data class CheckingPermission(override val transientSnackbar: String? = null) : ScanUiState

    data class Preview(
        val configId: String,
        override val transientSnackbar: String? = null,
    ) : ScanUiState

    data class PermissionDenied(
        val reason: String,
        override val transientSnackbar: String? = null,
    ) : ScanUiState

    data class NoCamera(override val transientSnackbar: String? = null) : ScanUiState

    data class Recognized(
        val studentId: String,
        override val transientSnackbar: String? = null,
    ) : ScanUiState
}
