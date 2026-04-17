package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.ResumeCandidate
import ai.wenjuanpro.app.data.result.StartSessionResult
import ai.wenjuanpro.app.domain.model.AppFailure
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import ai.wenjuanpro.app.domain.model.Session
import ai.wenjuanpro.app.domain.model.WriteFailedException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppendResultUseCaseTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private fun record(): ResultRecord =
        ResultRecord(
            qid = "Q1",
            type = QuestionType.SINGLE,
            mode = PresentMode.ALL_IN_ONE,
            stemMs = null,
            optionsMs = 1_000L,
            answer = "1",
            correct = "1",
            score = 10,
            status = ResultStatus.DONE,
        )

    private class StubRepo(private val behavior: suspend (ResultRecord) -> Result<Unit>) :
        ResultRepository {
        override suspend fun startSession(
            deviceId: String,
            studentId: String,
            configId: String,
            sessionStartMs: Long,
        ): StartSessionResult = StartSessionResult.Failure("not_used")

        override suspend fun hasIncompleteResult(
            deviceId: String,
            studentId: String,
            configId: String,
            totalQuestions: Int,
        ): Boolean = false

        override suspend fun openSession(session: Session) = Unit

        override suspend fun append(record: ResultRecord): Result<Unit> = behavior(record)

        override suspend fun findResumable(
            studentId: String,
            configId: String,
        ): ResumeCandidate? = null
    }

    @Test
    fun `2_3-UNIT-025 success returns Unit`() =
        runTest {
            val repo = StubRepo { Result.success(Unit) }
            val useCase = AppendResultUseCase(repo, dispatcher)
            val outcome = useCase(record())
            assertTrue(outcome.isSuccess)
        }

    @Test
    fun `2_3-UNIT-026 WriteFailedException maps to RESULT_WRITE_FAILED`() =
        runTest {
            val repo = StubRepo { Result.failure(WriteFailedException("disk full")) }
            val useCase = AppendResultUseCase(repo, dispatcher)
            val outcome = useCase(record())
            assertTrue(outcome.isFailure)
            val failure = outcome.exceptionOrNull()
            assertTrue("expected AppFailure got $failure", failure is AppFailure)
            assertEquals(
                AppendResultUseCase.CODE_RESULT_WRITE_FAILED,
                (failure as AppFailure).code,
            )
        }

    @Test
    fun `2_3-BLIND-ERROR-004 generic throwable also maps to RESULT_WRITE_FAILED`() =
        runTest {
            val repo = StubRepo { Result.failure(IllegalStateException("boom")) }
            val useCase = AppendResultUseCase(repo, dispatcher)
            val outcome = useCase(record())
            assertTrue(outcome.isFailure)
            val failure = outcome.exceptionOrNull()
            assertTrue(failure is AppFailure)
            assertEquals(
                AppendResultUseCase.CODE_RESULT_WRITE_FAILED,
                (failure as AppFailure).code,
            )
        }
}
