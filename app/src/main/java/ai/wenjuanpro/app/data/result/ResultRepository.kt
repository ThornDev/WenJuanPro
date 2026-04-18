package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.Session

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

    suspend fun openSession(session: Session)

    suspend fun append(record: ResultRecord): Result<Unit>

    suspend fun findResumable(
        studentId: String,
        configId: String,
    ): ResumeCandidate?
}

sealed interface StartSessionResult {
    data class Success(val sessionFileName: String) : StartSessionResult

    data class Failure(val code: String) : StartSessionResult
}

data class ResumeCandidate(
    val resultFileName: String,
    val completedQids: Set<String>,
    val cursor: Int = completedQids.size,
)
