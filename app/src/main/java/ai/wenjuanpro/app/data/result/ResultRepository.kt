package ai.wenjuanpro.app.data.result

interface ResultRepository {
    suspend fun startSession(
        deviceId: String,
        studentId: String,
        configId: String,
        sessionStartMs: Long,
    ): StartSessionResult

    suspend fun hasIncompleteResult(
        deviceId: String,
        studentId: String,
        configId: String,
        totalQuestions: Int,
    ): Boolean
}

sealed interface StartSessionResult {
    data class Success(val sessionFileName: String) : StartSessionResult

    data class Failure(val code: String) : StartSessionResult
}
