package ai.wenjuanpro.app.feature.complete

sealed interface CompleteUploadState {
    data object Idle : CompleteUploadState

    /** [attempt] is 1-based; null if no file is available to upload. */
    data class Uploading(val attempt: Int, val maxAttempts: Int) : CompleteUploadState

    data object Success : CompleteUploadState

    data class Failed(val attempts: Int, val reason: String? = null) : CompleteUploadState

    data object NoFile : CompleteUploadState
}
