package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.ResumeCandidate
import ai.wenjuanpro.app.data.result.StartSessionResult
import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.Session
import ai.wenjuanpro.app.domain.model.SsaidUnavailableException
import ai.wenjuanpro.app.domain.model.StemContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class StartSessionUseCaseTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private fun sampleConfig(configId: String = "cog-mem"): Config =
        Config(
            configId = configId,
            title = "Sample",
            sourceFileName = "$configId.txt",
            questions =
                listOf(
                    Question.SingleChoice(
                        qid = "Q1",
                        mode = PresentMode.ALL_IN_ONE,
                        stemDurationMs = null,
                        optionsDurationMs = 30_000L,
                        stem = StemContent.Text("stem"),
                        options = listOf(OptionContent.Text("A"), OptionContent.Text("B")),
                        correctIndex = 1,
                        scores = listOf(1, 0),
                    ),
                ),
        )

    private class FakeRepo(var openInvocations: Int = 0) : ResultRepository {
        var lastSession: Session? = null

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

        override suspend fun openSession(session: Session) {
            lastSession = session
            openInvocations += 1
        }

        override suspend fun append(record: ResultRecord): Result<Unit> = Result.success(Unit)

        override suspend fun findResumable(
            studentId: String,
            configId: String,
        ): ResumeCandidate? = null
    }

    @Test
    fun `2_3-UNIT-023 composes resultFileName per naming convention`() =
        runTest {
            val provider = DeviceIdProvider { "ABC" }
            val repo = FakeRepo()
            val useCase = StartSessionUseCase(provider, repo, dispatcher)
            val startAt = LocalDateTime.of(2026, 4, 18, 10, 30, 45)
            val result =
                useCase(
                    studentId = "U001",
                    config = sampleConfig(configId = "cog-mem"),
                    startAt = startAt,
                )
            assertTrue(result.isSuccess)
            val session = result.getOrThrow()
            assertEquals("ABC_U001_cog-mem_20260418-103045.txt", session.resultFileName)
            assertEquals(1, repo.openInvocations)
        }

    @Test
    fun `2_3-UNIT-024 SSAID unavailable returns failure`() =
        runTest {
            val provider = DeviceIdProvider { null }
            val repo = FakeRepo()
            val useCase = StartSessionUseCase(provider, repo, dispatcher)
            val result =
                useCase(
                    studentId = "U001",
                    config = sampleConfig(),
                    startAt = LocalDateTime.now(),
                )
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SsaidUnavailableException)
            assertEquals(0, repo.openInvocations)
        }

    @Test
    fun `SSAID blank treated as unavailable`() =
        runTest {
            val provider = DeviceIdProvider { "   " }
            val repo = FakeRepo()
            val useCase = StartSessionUseCase(provider, repo, dispatcher)
            val result =
                useCase(
                    studentId = "U001",
                    config = sampleConfig(),
                    startAt = LocalDateTime.now(),
                )
            assertTrue(result.isFailure)
        }
}
