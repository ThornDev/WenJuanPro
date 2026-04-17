package ai.wenjuanpro.app.feature.welcome

import ai.wenjuanpro.app.core.device.DeviceIdProvider
import ai.wenjuanpro.app.core.time.Clock
import ai.wenjuanpro.app.data.config.ConfigLoadResult
import ai.wenjuanpro.app.data.config.ConfigRepository
import ai.wenjuanpro.app.data.result.ResultRepository
import ai.wenjuanpro.app.data.result.StartSessionResult
import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.StemContent
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeConfirmViewModelTest {
    private lateinit var configRepo: ConfigRepository
    private lateinit var resultRepo: ResultRepository
    private lateinit var deviceIds: DeviceIdProvider
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        configRepo = mockk()
        resultRepo = mockk()
        deviceIds = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun singleChoice(
        qid: String,
        mode: PresentMode,
        stemMs: Long?,
        optionsMs: Long,
    ): Question.SingleChoice =
        Question.SingleChoice(
            qid = qid,
            mode = mode,
            stemDurationMs = stemMs,
            optionsDurationMs = optionsMs,
            stem = StemContent.Text("stem"),
            options = listOf(OptionContent.Text("A"), OptionContent.Text("B")),
            correctIndex = 0,
            scores = listOf(1, 0),
        )

    private fun sampleConfig(
        configId: String = CONFIG_ID,
        title: String = "认知记忆测评 v1",
        questions: List<Question> =
            listOf(
                singleChoice("q1", PresentMode.ALL_IN_ONE, stemMs = null, optionsMs = 60_000L),
                singleChoice("q2", PresentMode.STAGED, stemMs = 30_000L, optionsMs = 30_000L),
            ),
    ): Config =
        Config(
            configId = configId,
            title = title,
            sourceFileName = "$configId.txt",
            questions = questions,
        )

    private fun buildVm(
        studentId: String = STUDENT_ID,
        configId: String = CONFIG_ID,
        clock: Clock = FixedClock(FIXED_MILLIS),
    ): WelcomeConfirmViewModel {
        val handle =
            SavedStateHandle(
                mapOf(
                    WelcomeConfirmViewModel.KEY_STUDENT_ID to studentId,
                    WelcomeConfirmViewModel.KEY_CONFIG_ID to configId,
                ),
            )
        return WelcomeConfirmViewModel(
            configRepository = configRepo,
            resultRepository = resultRepo,
            deviceIdProvider = deviceIds,
            clock = clock,
            ioDispatcher = dispatcher,
            savedStateHandle = handle,
        )
    }

    private class FixedClock(private val now: Long) : Clock {
        override fun nowMs(): Long = now
    }

    @Test
    fun `2_2-UNIT-001 computeEtaMinutes same-screen and staged sum rounds up`() {
        val config =
            sampleConfig(
                questions =
                    listOf(
                        singleChoice("q1", PresentMode.ALL_IN_ONE, stemMs = null, optionsMs = 60_000L),
                        singleChoice("q2", PresentMode.STAGED, stemMs = 30_000L, optionsMs = 30_000L),
                        singleChoice("q3", PresentMode.ALL_IN_ONE, stemMs = null, optionsMs = 1L),
                    ),
            )
        assertEquals(3, WelcomeConfirmViewModel.computeEtaMinutes(config))
    }

    @Test
    fun `2_2-UNIT-002 computeEtaMinutes zero duration returns zero`() {
        val config = sampleConfig(questions = emptyList())
        assertEquals(0, WelcomeConfirmViewModel.computeEtaMinutes(config))
    }

    @Test
    fun `2_2-UNIT-010 ssaid unavailable on init enters SsaidUnavailable`() =
        runTest {
            every { deviceIds.ssaid() } returns null
            coEvery { configRepo.loadAll() } returns emptyList()
            coEvery { resultRepo.hasIncompleteResult(any(), any(), any(), any()) } returns false

            val vm = buildVm()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue("expected SsaidUnavailable got $state", state is WelcomeConfirmUiState.SsaidUnavailable)
        }

    @Test
    fun `2_2-UNIT-011 ssaid blank treated as unavailable`() =
        runTest {
            every { deviceIds.ssaid() } returns "   "
            coEvery { configRepo.loadAll() } returns emptyList()

            val vm = buildVm()
            advanceUntilIdle()

            assertTrue(vm.uiState.value is WelcomeConfirmUiState.SsaidUnavailable)
        }

    @Test
    fun `2_2-UNIT-020 happy path enters Ready with config info and eta`() =
        runTest {
            every { deviceIds.ssaid() } returns "ssaid-abc"
            coEvery { configRepo.loadAll() } returns listOf(ConfigLoadResult.Valid(sampleConfig()))
            coEvery {
                resultRepo.hasIncompleteResult("ssaid-abc", STUDENT_ID, CONFIG_ID, 2)
            } returns false

            val vm = buildVm()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue("expected Ready got $state", state is WelcomeConfirmUiState.Ready)
            val ready = state as WelcomeConfirmUiState.Ready
            assertEquals(STUDENT_ID, ready.studentId)
            assertEquals("认知记忆测评 v1", ready.title)
            assertEquals(2, ready.questionCount)
            assertEquals(2, ready.etaMinutes)
            assertFalse(ready.starting)
        }

    @Test
    fun `2_2-UNIT-030 incomplete result emits NavigateToResume effect`() =
        runTest {
            every { deviceIds.ssaid() } returns "ssaid-abc"
            coEvery { configRepo.loadAll() } returns listOf(ConfigLoadResult.Valid(sampleConfig()))
            coEvery { resultRepo.hasIncompleteResult(any(), any(), any(), any()) } returns true

            val vm = buildVm()
            val effects = collectEffects(vm)
            advanceUntilIdle()
            effects.cancel()

            assertEquals(1, effects.items.count { it is WelcomeConfirmEffect.NavigateToResume })
            val target = effects.items.filterIsInstance<WelcomeConfirmEffect.NavigateToResume>().first()
            assertEquals(STUDENT_ID, target.studentId)
            assertEquals(CONFIG_ID, target.configId)
        }

    @Test
    fun `2_2-UNIT-040 onStartClicked records sessionStart and navigates to first question`() =
        runTest {
            every { deviceIds.ssaid() } returns "ssaid-abc"
            coEvery { configRepo.loadAll() } returns listOf(ConfigLoadResult.Valid(sampleConfig()))
            coEvery { resultRepo.hasIncompleteResult(any(), any(), any(), any()) } returns false
            coEvery {
                resultRepo.startSession("ssaid-abc", STUDENT_ID, CONFIG_ID, FIXED_MILLIS)
            } returns StartSessionResult.Success("file.txt")

            val vm = buildVm(clock = FixedClock(FIXED_MILLIS))
            advanceUntilIdle()
            val effects = collectEffects(vm)
            vm.onIntent(WelcomeConfirmIntent.OnStartClicked)
            advanceUntilIdle()
            effects.cancel()

            coVerify(exactly = 1) {
                resultRepo.startSession("ssaid-abc", STUDENT_ID, CONFIG_ID, FIXED_MILLIS)
            }
            assertEquals(
                1,
                effects.items.count { it is WelcomeConfirmEffect.NavigateToFirstQuestion },
            )
        }

    @Test
    fun `2_2-UNIT-041 onStartClicked dedup prevents duplicate startSession`() =
        runTest {
            every { deviceIds.ssaid() } returns "ssaid-abc"
            coEvery { configRepo.loadAll() } returns listOf(ConfigLoadResult.Valid(sampleConfig()))
            coEvery { resultRepo.hasIncompleteResult(any(), any(), any(), any()) } returns false
            coEvery {
                resultRepo.startSession(any(), any(), any(), any())
            } returns StartSessionResult.Success("file.txt")

            val vm = buildVm()
            advanceUntilIdle()
            vm.onIntent(WelcomeConfirmIntent.OnStartClicked)
            vm.onIntent(WelcomeConfirmIntent.OnStartClicked)
            vm.onIntent(WelcomeConfirmIntent.OnStartClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) { resultRepo.startSession(any(), any(), any(), any()) }
        }

    @Test
    fun `2_2-UNIT-050 missing args falls into ConfigMissing`() =
        runTest {
            val vm = buildVm(studentId = "", configId = "")
            advanceUntilIdle()

            assertTrue(vm.uiState.value is WelcomeConfirmUiState.ConfigMissing)
        }

    @Test
    fun `2_2-UNIT-051 config not found in repo enters ConfigMissing`() =
        runTest {
            every { deviceIds.ssaid() } returns "ssaid-abc"
            coEvery { configRepo.loadAll() } returns
                listOf(ConfigLoadResult.Valid(sampleConfig(configId = "other")))

            val vm = buildVm()
            advanceUntilIdle()

            assertTrue(vm.uiState.value is WelcomeConfirmUiState.ConfigMissing)
        }

    @Test
    fun `2_2-UNIT-060 retry after ssaid failure re-reads provider`() =
        runTest {
            every { deviceIds.ssaid() } returnsMany listOf(null, "ssaid-abc")
            coEvery { configRepo.loadAll() } returns listOf(ConfigLoadResult.Valid(sampleConfig()))
            coEvery { resultRepo.hasIncompleteResult(any(), any(), any(), any()) } returns false

            val vm = buildVm()
            advanceUntilIdle()
            assertTrue(vm.uiState.value is WelcomeConfirmUiState.SsaidUnavailable)

            vm.onIntent(WelcomeConfirmIntent.OnRetrySsaid)
            advanceUntilIdle()

            assertTrue(vm.uiState.value is WelcomeConfirmUiState.Ready)
        }

    private class EffectCollector(
        val items: MutableList<WelcomeConfirmEffect>,
        private val cancel: () -> Unit,
    ) {
        fun cancel() = cancel.invoke()
    }

    private fun TestScope.collectEffects(vm: WelcomeConfirmViewModel): EffectCollector {
        val items = mutableListOf<WelcomeConfirmEffect>()
        val job = launch { vm.effects.toList(items) }
        return EffectCollector(items) { job.cancel() }
    }

    private companion object {
        const val STUDENT_ID = "S001"
        const val CONFIG_ID = "cog-mem-2026q3"
        const val FIXED_MILLIS = 1_713_000_000_000L
    }
}
