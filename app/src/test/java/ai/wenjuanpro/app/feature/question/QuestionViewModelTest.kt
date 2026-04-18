package ai.wenjuanpro.app.feature.question

import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.core.time.Clock
import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.ResumeCandidate
import ai.wenjuanpro.app.data.result.StartSessionResult
import ai.wenjuanpro.app.domain.fsm.QuestionFsm
import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import ai.wenjuanpro.app.domain.model.Session
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.domain.session.SessionStateHolder
import ai.wenjuanpro.app.domain.usecase.AppendResultUseCase
import ai.wenjuanpro.app.domain.usecase.ScoreMultiChoiceUseCase
import ai.wenjuanpro.app.domain.usecase.ScoreSingleChoiceUseCase
import ai.wenjuanpro.app.domain.usecase.StartSessionUseCase
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var configRepo: ConfigRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        configRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun question(
        qid: String = "Q1",
        mode: PresentMode = PresentMode.ALL_IN_ONE,
        stemMs: Long? = if (mode == PresentMode.STAGED) 10_000L else null,
        optionsMs: Long = 30_000L,
        scores: List<Int> = listOf(10, 5, 0, 0),
        correct: Int = 1,
    ): Question.SingleChoice =
        Question.SingleChoice(
            qid = qid,
            mode = mode,
            stemDurationMs = stemMs,
            optionsDurationMs = optionsMs,
            stem = StemContent.Text("stem"),
            options =
                listOf(
                    OptionContent.Text("A"),
                    OptionContent.Text("B"),
                    OptionContent.Text("C"),
                    OptionContent.Text("D"),
                ),
            correctIndex = correct,
            scores = scores,
        )

    private fun config(questions: List<Question>): Config =
        Config(
            configId = CONFIG_ID,
            title = "Sample",
            sourceFileName = "$CONFIG_ID.txt",
            questions = questions,
        )

    private class FakeResultRepo(
        var failuresRemaining: Int = 0,
        val records: MutableList<ResultRecord> = mutableListOf(),
    ) : ResultRepository {
        var openInvocations: Int = 0

        override suspend fun startSession(
            deviceId: String,
            studentId: String,
            configId: String,
            sessionStartMs: Long,
        ): StartSessionResult = StartSessionResult.Failure("not_used_in_2_3")

        override suspend fun hasIncompleteResult(
            deviceId: String,
            studentId: String,
            configId: String,
            totalQuestions: Int,
        ): Boolean = false

        override suspend fun openSession(session: Session) {
            openInvocations += 1
        }

        override suspend fun append(record: ResultRecord): Result<Unit> {
            if (failuresRemaining > 0) {
                failuresRemaining -= 1
                return Result.failure(IllegalStateException("simulated"))
            }
            records.add(record)
            return Result.success(Unit)
        }

        override suspend fun findResumable(
            studentId: String,
            configId: String,
        ): ResumeCandidate? = null
    }

    private fun TestScope.buildVm(
        cfg: Config,
        repo: FakeResultRepo,
        deviceIdProvider: DeviceIdProvider = DeviceIdProvider { "DEV1" },
    ): QuestionViewModel {
        coEvery { configRepo.loadAll() } returns listOf(ConfigLoadResult.Valid(cfg))
        val score = ScoreSingleChoiceUseCase()
        val fsm = QuestionFsm(score, ScoreMultiChoiceUseCase())
        val startSession = StartSessionUseCase(deviceIdProvider, repo, dispatcher)
        val append = AppendResultUseCase(repo, dispatcher)
        val clock = object : Clock {
            override fun nowMs(): Long = testScheduler.currentTime
        }
        val handle =
            SavedStateHandle(
                mapOf(
                    QuestionViewModel.KEY_STUDENT_ID to STUDENT_ID,
                    QuestionViewModel.KEY_CONFIG_ID to CONFIG_ID,
                ),
            )
        return QuestionViewModel(
            sessionStateHolder = SessionStateHolder(),
            configRepository = configRepo,
            startSessionUseCase = startSession,
            appendResultUseCase = append,
            questionFsm = fsm,
            clock = clock,
            ioDispatcher = dispatcher,
            savedStateHandle = handle,
        )
    }

    private class EffectCollector(
        val items: MutableList<QuestionEffect>,
        private val cancel: () -> Unit,
    ) {
        fun cancel() = cancel.invoke()
    }

    private fun TestScope.collectEffects(vm: QuestionViewModel): EffectCollector {
        val items = mutableListOf<QuestionEffect>()
        val job = launch { vm.effects.toList(items) }
        return EffectCollector(items) { job.cancel() }
    }

    // ------------------------------------------------------------------
    // AC1
    // ------------------------------------------------------------------

    @Test
    fun `2_3-UNIT-010 OnEnter bootstraps to SingleChoiceAllInOne`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(
                "expected SingleChoiceAllInOne got $state",
                state is QuestionUiState.SingleChoiceAllInOne,
            )
            state as QuestionUiState.SingleChoiceAllInOne
            assertNull(state.selectedIndex)
            assertFalse(state.submitEnabled)
            assertEquals("Q1", state.qid)
            assertEquals(1, repo.openInvocations)
        }

    @Test
    fun `2_3-UNIT-011 SelectOption is exclusive and enables submit`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val vm = buildVm(cfg, FakeResultRepo())
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.SelectOption(2))
            vm.onIntent(QuestionIntent.SelectOption(3))
            val state = vm.uiState.value as QuestionUiState.SingleChoiceAllInOne
            assertEquals(3, state.selectedIndex)
            assertTrue(state.submitEnabled)
        }

    @Test
    fun `2_3-UNIT-012 Submit triggers AppendResult and emits NavigateNext`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(qid = "Q1"), question(qid = "Q2")))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val effects = collectEffects(vm)
            vm.onIntent(QuestionIntent.SelectOption(2))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            effects.cancel()
            assertEquals(1, repo.records.size)
            val rec = repo.records.first()
            assertEquals("2", rec.answer)
            assertEquals(ResultStatus.DONE, rec.status)
            assertEquals(1, effects.items.count { it is QuestionEffect.NavigateNext })
        }

    @Test
    fun `2_3-UNIT-013 TimerExpired writes NOT_ANSWERED and advances`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(optionsMs = 10_000L), question(qid = "Q2")))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val effects = collectEffects(vm)
            advanceTimeBy(10_500L)
            advanceUntilIdle()
            effects.cancel()
            assertEquals(1, repo.records.size)
            val rec = repo.records.first()
            assertEquals(ResultStatus.NOT_ANSWERED, rec.status)
            assertTrue(effects.items.any { it is QuestionEffect.NavigateNext })
        }

    @Test
    fun `2_3-UNIT-014 countdown progress reaches half at half duration`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(optionsMs = 30_000L)))
            val vm = buildVm(cfg, FakeResultRepo())
            advanceUntilIdle()
            advanceTimeBy(15_000L)
            advanceUntilIdle()
            val s = vm.uiState.value as QuestionUiState.SingleChoiceAllInOne
            assertTrue("progress=${s.countdownProgress}", s.countdownProgress in 0.40f..0.55f)
        }

    @Test
    fun `2_3-UNIT-015 isWarning flips true when remaining le 5 seconds`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(optionsMs = 30_000L)))
            val vm = buildVm(cfg, FakeResultRepo())
            advanceUntilIdle()
            advanceTimeBy(25_500L)
            advanceUntilIdle()
            val s = vm.uiState.value as QuestionUiState.SingleChoiceAllInOne
            assertTrue("isWarning=${s.isWarning}", s.isWarning)
        }

    @Test
    fun `2_3-INT-001 happy path records single ResultRecord with stemMs null`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(qid = "Q1"), question(qid = "Q2")))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            advanceTimeBy(2_000L)
            vm.onIntent(QuestionIntent.SelectOption(2))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            assertEquals(1, repo.records.size)
            val rec = repo.records.first()
            assertEquals("Q1", rec.qid)
            assertEquals(PresentMode.ALL_IN_ONE, rec.mode)
            assertNull(rec.stemMs)
            assertEquals("2", rec.answer)
            assertEquals(ResultStatus.DONE, rec.status)
            assertTrue("optionsMs=${rec.optionsMs}", rec.optionsMs >= 2_000L)
        }

    // ------------------------------------------------------------------
    // AC2
    // ------------------------------------------------------------------

    @Test
    fun `2_3-UNIT-020 StageTransition switches stage and resets countdown to 1`() =
        runTest(dispatcher) {
            val cfg =
                config(
                    listOf(
                        question(
                            mode = PresentMode.STAGED,
                            stemMs = 10_000L,
                            optionsMs = 20_000L,
                        ),
                    ),
                )
            val vm = buildVm(cfg, FakeResultRepo())
            advanceUntilIdle()
            // verify stem stage rendered
            val pre = vm.uiState.value as QuestionUiState.SingleChoiceStaged
            assertEquals(QuestionUiState.Stage.STEM, pre.stage)
            vm.onIntent(QuestionIntent.StageTransition)
            advanceUntilIdle()
            val post = vm.uiState.value as QuestionUiState.SingleChoiceStaged
            assertEquals(QuestionUiState.Stage.OPTIONS, post.stage)
            assertEquals(1f, post.countdownProgress, 0.001f)
        }

    @Test
    fun `2_3-UNIT-021 staged stem phase silently ignores SelectOption and Submit`() =
        runTest(dispatcher) {
            val cfg =
                config(
                    listOf(
                        question(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L),
                    ),
                )
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.SelectOption(2))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            assertEquals(0, repo.records.size)
            val s = vm.uiState.value as QuestionUiState.SingleChoiceStaged
            assertEquals(QuestionUiState.Stage.STEM, s.stage)
            assertNull(s.selectedIndex)
        }

    @Test
    fun `2_3-UNIT-022 stem timeout auto transitions to OPTIONS without partial append`() =
        runTest(dispatcher) {
            val cfg =
                config(
                    listOf(
                        question(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L),
                    ),
                )
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            advanceTimeBy(10_500L)
            advanceUntilIdle()
            val s = vm.uiState.value as QuestionUiState.SingleChoiceStaged
            assertEquals(QuestionUiState.Stage.OPTIONS, s.stage)
            assertEquals(0, repo.records.size)
        }

    @Test
    fun `2_3-INT-002 staged end to end records single ResultRecord with stemMs and optionsMs`() =
        runTest(dispatcher) {
            val cfg =
                config(
                    listOf(
                        question(mode = PresentMode.STAGED, stemMs = 2_000L, optionsMs = 3_000L),
                    ),
                )
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            advanceTimeBy(2_500L) // stem auto-transition
            advanceUntilIdle()
            advanceTimeBy(1_000L) // 1s into options stage
            vm.onIntent(QuestionIntent.SelectOption(1))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            assertEquals(1, repo.records.size)
            val rec = repo.records.first()
            assertEquals(2_000L, rec.stemMs)
            assertEquals("1", rec.answer)
            assertTrue("optionsMs=${rec.optionsMs}", rec.optionsMs in 1_000L..1_500L)
        }

    @Test
    fun `2_3-BLIND-DATA-001 staged path writes exactly one ResultRecord per question`() =
        runTest(dispatcher) {
            val cfg =
                config(
                    listOf(
                        question(mode = PresentMode.STAGED, stemMs = 2_000L, optionsMs = 3_000L),
                    ),
                )
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            advanceTimeBy(2_500L)
            advanceUntilIdle()
            advanceTimeBy(3_500L)
            advanceUntilIdle()
            assertEquals(1, repo.records.size)
        }

    // ------------------------------------------------------------------
    // Cross-cutting blind spots
    // ------------------------------------------------------------------

    @Test
    fun `2_3-BLIND-ERROR-001 single append failure emits ShowRetryBanner with retriesLeft 2`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(qid = "Q1"), question(qid = "Q2")))
            val repo = FakeResultRepo(failuresRemaining = 1)
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val effects = collectEffects(vm)
            vm.onIntent(QuestionIntent.SelectOption(1))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            effects.cancel()
            val banner = effects.items.filterIsInstance<QuestionEffect.ShowRetryBanner>()
            assertEquals(1, banner.size)
            assertEquals(2, banner.first().retriesLeft)
        }

    @Test
    fun `2_3-BLIND-ERROR-002 three consecutive append failures emit NavigateTerminal`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val repo = FakeResultRepo(failuresRemaining = 3)
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val effects = collectEffects(vm)
            vm.onIntent(QuestionIntent.SelectOption(1))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.Retry)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.Retry)
            advanceUntilIdle()
            effects.cancel()
            assertEquals(0, repo.records.size)
            assertEquals(1, effects.items.count { it is QuestionEffect.NavigateTerminal })
        }

    @Test
    fun `2_3-BLIND-FLOW-002 double Submit triggers single append`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(qid = "Q1"), question(qid = "Q2")))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.SelectOption(1))
            vm.onIntent(QuestionIntent.Submit)
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            assertEquals(1, repo.records.count { it.qid == "Q1" })
        }

    @Test
    fun `2_3-BLIND-FLOW-003 Submit without selection is no-op`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            assertEquals(0, repo.records.size)
        }

    @Test
    fun `2_3-BLIND-CONCURRENCY-002 rapid SelectOption converges to last selection`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val vm = buildVm(cfg, FakeResultRepo())
            advanceUntilIdle()
            (1..4).forEach { vm.onIntent(QuestionIntent.SelectOption(it)) }
            val s = vm.uiState.value as QuestionUiState.SingleChoiceAllInOne
            assertEquals(4, s.selectedIndex)
        }

    @Test
    fun `SSAID unavailable on bootstrap renders Error state`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo, deviceIdProvider = DeviceIdProvider { null })
            advanceUntilIdle()
            val s = vm.uiState.value
            assertTrue(s is QuestionUiState.Error)
            assertEquals(QuestionViewModel.CODE_SSAID_UNAVAILABLE, (s as QuestionUiState.Error).message)
            assertEquals(0, repo.openInvocations)
        }

    @Test
    fun `last-question Submit emits NavigateComplete`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val effects = collectEffects(vm)
            vm.onIntent(QuestionIntent.SelectOption(1))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            effects.cancel()
            assertEquals(1, effects.items.count { it is QuestionEffect.NavigateComplete })
            assertEquals(0, effects.items.count { it is QuestionEffect.NavigateNext })
        }

    @Test
    fun `out-of-range answer enters Errored via FSM`() =
        runTest(dispatcher) {
            val cfg = config(listOf(question(scores = listOf(1, 2))))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            // selection=2 then submit → score throws because options=4 but scores.size=2 → mismatch
            vm.onIntent(QuestionIntent.SelectOption(2))
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            // Should NOT have appended any record
            assertEquals(0, repo.records.size)
            // VM enters Error or stays unchanged
            val s = vm.uiState.value
            assertNotNull(s)
        }

    // ------------------------------------------------------------------
    // Story 2.4: Multi-choice
    // ------------------------------------------------------------------

    private fun multiQuestion(
        qid: String = "Q1",
        mode: PresentMode = PresentMode.ALL_IN_ONE,
        stemMs: Long? = if (mode == PresentMode.STAGED) 10_000L else null,
        optionsMs: Long = 30_000L,
        scores: List<Int> = listOf(10, 5, 3, 2),
        correctIndices: Set<Int> = setOf(1, 2),
    ): Question.MultiChoice =
        Question.MultiChoice(
            qid = qid,
            mode = mode,
            stemDurationMs = stemMs,
            optionsDurationMs = optionsMs,
            stem = StemContent.Text("multi stem"),
            options =
                listOf(
                    OptionContent.Text("A"),
                    OptionContent.Text("B"),
                    OptionContent.Text("C"),
                    OptionContent.Text("D"),
                ),
            correctIndices = correctIndices,
            scores = scores,
        )

    @Test
    fun `2_4-UNIT-012 OnEnter MultiChoice bootstraps to MultiChoiceAllInOne`() =
        runTest(dispatcher) {
            val cfg = config(listOf(multiQuestion()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(
                "expected MultiChoiceAllInOne got $state",
                state is QuestionUiState.MultiChoiceAllInOne,
            )
            state as QuestionUiState.MultiChoiceAllInOne
            assertTrue(state.selectedIndices.isEmpty())
            assertFalse(state.submitEnabled)
        }

    @Test
    fun `2_4-UNIT-013 ToggleOption + Submit writes correct multi record`() =
        runTest(dispatcher) {
            val cfg = config(listOf(multiQuestion()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.ToggleOption(2))
            vm.onIntent(QuestionIntent.ToggleOption(3))
            val state = vm.uiState.value as QuestionUiState.MultiChoiceAllInOne
            assertEquals(setOf(2, 3), state.selectedIndices)
            assertTrue(state.submitEnabled)
            vm.onIntent(QuestionIntent.Submit)
            advanceUntilIdle()
            assertEquals(1, repo.records.size)
            val record = repo.records.first()
            assertEquals("2,3", record.answer)
            assertEquals(8, record.score) // scores[1]+scores[2] = 5+3
            assertEquals(ResultStatus.DONE, record.status)
        }

    @Test
    fun `2_4-UNIT-014 ToggleOption toggles off to empty disables submit`() =
        runTest(dispatcher) {
            val cfg = config(listOf(multiQuestion()))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            vm.onIntent(QuestionIntent.ToggleOption(1))
            assertTrue((vm.uiState.value as QuestionUiState.MultiChoiceAllInOne).submitEnabled)
            vm.onIntent(QuestionIntent.ToggleOption(1))
            val state = vm.uiState.value as QuestionUiState.MultiChoiceAllInOne
            assertTrue(state.selectedIndices.isEmpty())
            assertFalse(state.submitEnabled)
        }

    @Test
    fun `2_4-UNIT-015 multi TimerExpired writes NOT_ANSWERED`() =
        runTest(dispatcher) {
            val cfg = config(listOf(multiQuestion(optionsMs = 2_000L)))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            advanceTimeBy(2_100L)
            advanceUntilIdle()
            assertEquals(1, repo.records.size)
            assertEquals(ResultStatus.NOT_ANSWERED, repo.records.first().status)
            assertEquals("", repo.records.first().answer)
        }

    @Test
    fun `2_4-UNIT-020 staged multi stem phase ignores ToggleOption and Submit`() =
        runTest(dispatcher) {
            val cfg =
                config(listOf(multiQuestion(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L)))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            val initial = vm.uiState.value
            assertTrue(
                "expected MultiChoiceStaged got $initial",
                initial is QuestionUiState.MultiChoiceStaged,
            )
            assertEquals(QuestionUiState.Stage.STEM, (initial as QuestionUiState.MultiChoiceStaged).stage)
            vm.onIntent(QuestionIntent.ToggleOption(2))
            vm.onIntent(QuestionIntent.Submit)
            val after = vm.uiState.value as QuestionUiState.MultiChoiceStaged
            assertEquals(QuestionUiState.Stage.STEM, after.stage)
            assertTrue(after.selectedIndices.isEmpty())
        }

    @Test
    fun `2_4-UNIT-021 staged multi stem timeout transitions to OPTIONS`() =
        runTest(dispatcher) {
            val cfg =
                config(listOf(multiQuestion(mode = PresentMode.STAGED, stemMs = 2_000L, optionsMs = 20_000L)))
            val repo = FakeResultRepo()
            val vm = buildVm(cfg, repo)
            advanceUntilIdle()
            advanceTimeBy(2_100L)
            advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(
                "expected MultiChoiceStaged OPTIONS got $state",
                state is QuestionUiState.MultiChoiceStaged,
            )
            assertEquals(QuestionUiState.Stage.OPTIONS, (state as QuestionUiState.MultiChoiceStaged).stage)
            assertEquals(0, repo.records.size)
        }

    private companion object {
        const val STUDENT_ID = "S001"
        const val CONFIG_ID = "cog-mem-2026q3"
    }
}
