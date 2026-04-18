package ai.wenjuanpro.app.domain.fsm

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.ResultStatus
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.domain.usecase.ScoreMultiChoiceUseCase
import ai.wenjuanpro.app.domain.usecase.ScoreSingleChoiceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionFsmTest {
    private lateinit var fsm: QuestionFsm

    @Before
    fun setUp() {
        fsm =
            QuestionFsm(
                score = ScoreSingleChoiceUseCase(),
                scoreMulti = ScoreMultiChoiceUseCase(),
            )
    }

    private fun singleChoice(
        mode: PresentMode = PresentMode.ALL_IN_ONE,
        stemMs: Long? = if (mode == PresentMode.STAGED) 10_000L else null,
        optionsMs: Long = 30_000L,
        scores: List<Int> = listOf(10, 5, 0, 0),
        correct: Int = 1,
        qid: String = "Q1",
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

    @Test
    fun `2_3-UNIT-004 Enter all_in_one transitions to QuestionAllInOne`() {
        val q = singleChoice()
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), nowMs = 1_000L)
        assertTrue(s is QuestionFsmState.QuestionAllInOne)
        s as QuestionFsmState.QuestionAllInOne
        assertEquals(q, s.question)
        assertEquals(1_000L, s.stageEnteredMs)
        assertEquals(null, s.selectedIndex)
    }

    @Test
    fun `2_3-UNIT-005 OptionsSubmit moves to Writing with answer`() {
        val q = singleChoice()
        val entered =
            fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), nowMs = 0L)
        val writing =
            fsm.reduce(entered, QuestionEvent.OptionsSubmit(2), nowMs = 4_000L)
        assertTrue(writing is QuestionFsmState.Writing)
        writing as QuestionFsmState.Writing
        assertEquals("2", writing.record.answer)
        assertEquals(ResultStatus.DONE, writing.record.status)
        assertEquals(4_000L, writing.record.optionsMs)
        assertEquals(3, writing.retriesLeft)
    }

    @Test
    fun `2_3-UNIT-006 OptionsTimeout moves to Writing NOT_ANSWERED`() {
        val q = singleChoice()
        val entered =
            fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), nowMs = 0L)
        val writing = fsm.reduce(entered, QuestionEvent.OptionsTimeout, nowMs = 30_000L)
        assertTrue(writing is QuestionFsmState.Writing)
        writing as QuestionFsmState.Writing
        assertEquals("", writing.record.answer)
        assertEquals(0, writing.record.score)
        assertEquals(ResultStatus.NOT_ANSWERED, writing.record.status)
        assertEquals(30_000L, writing.record.optionsMs)
    }

    @Test
    fun `2_3-UNIT-007 WriteSuccess transitions to NextDecision`() {
        val q = singleChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val writing = fsm.reduce(entered, QuestionEvent.OptionsSubmit(1), 1_000L)
        val next = fsm.reduce(writing, QuestionEvent.WriteSuccess, 1_001L)
        assertTrue(next is QuestionFsmState.NextDecision)
        assertEquals("Q1", (next as QuestionFsmState.NextDecision).lastQid)
    }

    @Test
    fun `2_3-UNIT-008 WriteFailure decrements retriesLeft`() {
        val q = singleChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val writing = fsm.reduce(entered, QuestionEvent.OptionsSubmit(1), 1_000L)
        val errored = fsm.reduce(writing, QuestionEvent.WriteFailure, 1_001L)
        assertTrue(errored is QuestionFsmState.WriteError)
        assertEquals(2, (errored as QuestionFsmState.WriteError).retriesLeft)
    }

    @Test
    fun `2_3-UNIT-009 RetryExhausted at zero retries transitions to Terminated`() {
        val q = singleChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val writing = fsm.reduce(entered, QuestionEvent.OptionsSubmit(1), 1_000L)
        var s: QuestionFsmState = writing
        s = fsm.reduce(s, QuestionEvent.WriteFailure, 2_000L)
        s = fsm.reduce(s, QuestionEvent.Retry, 2_001L)
        s = fsm.reduce(s, QuestionEvent.WriteFailure, 3_000L)
        s = fsm.reduce(s, QuestionEvent.Retry, 3_001L)
        s = fsm.reduce(s, QuestionEvent.WriteFailure, 4_000L)
        assertTrue(s is QuestionFsmState.WriteError)
        assertEquals(0, (s as QuestionFsmState.WriteError).retriesLeft)
        s = fsm.reduce(s, QuestionEvent.RetryExhausted, 4_001L)
        assertEquals(QuestionFsmState.Terminated, s)
    }

    @Test
    fun `2_3-UNIT-016 Enter staged transitions to QuestionStagedStem`() {
        val q = singleChoice(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L)
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), nowMs = 0L)
        assertTrue(s is QuestionFsmState.QuestionStagedStem)
        s as QuestionFsmState.QuestionStagedStem
        assertEquals(0L, s.stageEnteredMs)
    }

    @Test
    fun `2_3-UNIT-017 StemTimeout freezes stemMs at stemDurationMs`() {
        val q = singleChoice(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val options = fsm.reduce(stem, QuestionEvent.StemTimeout, nowMs = 10_000L)
        assertTrue(options is QuestionFsmState.QuestionStagedOptions)
        options as QuestionFsmState.QuestionStagedOptions
        assertEquals(10_000L, options.stemMs)
        assertEquals(10_000L, options.stageEnteredMs)
    }

    @Test
    fun `2_3-UNIT-018 staged OptionsSubmit captures optionsMs`() {
        val q = singleChoice(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val options = fsm.reduce(stem, QuestionEvent.StemTimeout, nowMs = 10_000L)
        val writing = fsm.reduce(options, QuestionEvent.OptionsSubmit(1), nowMs = 18_000L)
        assertTrue(writing is QuestionFsmState.Writing)
        writing as QuestionFsmState.Writing
        assertEquals("1", writing.record.answer)
        assertEquals(10_000L, writing.record.stemMs)
        assertEquals(8_000L, writing.record.optionsMs)
    }

    @Test
    fun `2_3-UNIT-019 staged OptionsTimeout records not_answered`() {
        val q = singleChoice(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val options = fsm.reduce(stem, QuestionEvent.StemTimeout, 10_000L)
        val writing = fsm.reduce(options, QuestionEvent.OptionsTimeout, 30_000L)
        assertTrue(writing is QuestionFsmState.Writing)
        writing as QuestionFsmState.Writing
        assertEquals("", writing.record.answer)
        assertEquals(10_000L, writing.record.stemMs)
        assertEquals(20_000L, writing.record.optionsMs)
        assertEquals(ResultStatus.NOT_ANSWERED, writing.record.status)
    }

    @Test
    fun `2_3-BLIND-CONCURRENCY-001 OptionsTimeout after Writing is dropped (single append)`() {
        val q = singleChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        val writing = fsm.reduce(entered, QuestionEvent.OptionsSubmit(2), 1_000L)
        val again = fsm.reduce(writing, QuestionEvent.OptionsTimeout, 1_001L)
        assertEquals(writing, again)
    }

    @Test
    fun `2_3-BLIND-CONCURRENCY-002 SelectOption spam converges to last selection`() {
        val q = singleChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        var s: QuestionFsmState = entered
        s = fsm.reduce(s, QuestionEvent.SelectOption(1), 100L)
        s = fsm.reduce(s, QuestionEvent.SelectOption(3), 200L)
        s = fsm.reduce(s, QuestionEvent.SelectOption(2), 300L)
        assertTrue(s is QuestionFsmState.QuestionAllInOne)
        assertEquals(2, (s as QuestionFsmState.QuestionAllInOne).selectedIndex)
    }

    @Test
    fun `Enter staged with null stemDurationMs transitions to Errored`() {
        val q = singleChoice(mode = PresentMode.STAGED, stemMs = null, optionsMs = 20_000L)
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.Enter(q), 0L)
        assertTrue(s is QuestionFsmState.Errored)
    }

    // ============================================================
    // Multi-choice FSM tests (Story 2.4)
    // ============================================================

    private fun multiChoice(
        mode: PresentMode = PresentMode.ALL_IN_ONE,
        stemMs: Long? = if (mode == PresentMode.STAGED) 10_000L else null,
        optionsMs: Long = 30_000L,
        scores: List<Int> = listOf(10, 5, 3, 2),
        correctIndices: Set<Int> = setOf(1, 2),
        qid: String = "Q1",
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
    fun `2_4-UNIT-006 EnterMulti all_in_one transitions to MultiAllInOne`() {
        val q = multiChoice()
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 1_000L)
        assertTrue(s is QuestionFsmState.MultiAllInOne)
        s as QuestionFsmState.MultiAllInOne
        assertEquals(q, s.question)
        assertEquals(1_000L, s.stageEnteredMs)
        assertTrue(s.selectedIndices.isEmpty())
    }

    @Test
    fun `2_4-UNIT-007 ToggleOption adds index to selectedIndices`() {
        val q = multiChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val toggled = fsm.reduce(entered, QuestionEvent.ToggleOption(2), 100L)
        assertTrue(toggled is QuestionFsmState.MultiAllInOne)
        assertEquals(setOf(2), (toggled as QuestionFsmState.MultiAllInOne).selectedIndices)
    }

    @Test
    fun `2_4-UNIT-008 ToggleOption removes existing index`() {
        val q = multiChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val added = fsm.reduce(entered, QuestionEvent.ToggleOption(2), 100L)
        val addAnother = fsm.reduce(added, QuestionEvent.ToggleOption(3), 200L)
        val removed = fsm.reduce(addAnother, QuestionEvent.ToggleOption(3), 300L)
        assertTrue(removed is QuestionFsmState.MultiAllInOne)
        assertEquals(setOf(2), (removed as QuestionFsmState.MultiAllInOne).selectedIndices)
    }

    @Test
    fun `2_4-UNIT-009 ToggleOption to empty set clears selectedIndices`() {
        val q = multiChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val added = fsm.reduce(entered, QuestionEvent.ToggleOption(2), 100L)
        val removed = fsm.reduce(added, QuestionEvent.ToggleOption(2), 200L)
        assertTrue(removed is QuestionFsmState.MultiAllInOne)
        assertTrue((removed as QuestionFsmState.MultiAllInOne).selectedIndices.isEmpty())
    }

    @Test
    fun `2_4-UNIT-010 MultiOptionsSubmit produces Writing with scored record`() {
        val q = multiChoice(scores = listOf(10, 5, 3, 2))
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val toggled = fsm.reduce(entered, QuestionEvent.ToggleOption(1), 100L)
        val toggled2 = fsm.reduce(toggled, QuestionEvent.ToggleOption(3), 200L)
        val writing =
            fsm.reduce(toggled2, QuestionEvent.MultiOptionsSubmit(setOf(1, 3)), 5_000L)
        assertTrue(writing is QuestionFsmState.Writing)
        writing as QuestionFsmState.Writing
        assertEquals("1,3", writing.record.answer)
        assertEquals(13, writing.record.score)
        assertEquals(ResultStatus.DONE, writing.record.status)
    }

    @Test
    fun `2_4-UNIT-011 MultiAllInOne OptionsTimeout produces Writing NOT_ANSWERED`() {
        val q = multiChoice()
        val entered = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val timeout = fsm.reduce(entered, QuestionEvent.OptionsTimeout, 30_000L)
        assertTrue(timeout is QuestionFsmState.Writing)
        timeout as QuestionFsmState.Writing
        assertEquals("", timeout.record.answer)
        assertEquals(0, timeout.record.score)
        assertEquals(ResultStatus.NOT_ANSWERED, timeout.record.status)
    }

    @Test
    fun `2_4-UNIT-016 EnterMulti staged transitions to MultiStagedStem`() {
        val q = multiChoice(mode = PresentMode.STAGED)
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        assertTrue(s is QuestionFsmState.MultiStagedStem)
        s as QuestionFsmState.MultiStagedStem
        assertEquals(q, s.question)
    }

    @Test
    fun `2_4-UNIT-017 MultiStagedStem StemTimeout produces MultiStagedOptions with frozen stemMs`() {
        val q = multiChoice(mode = PresentMode.STAGED, stemMs = 10_000L)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val opts = fsm.reduce(stem, QuestionEvent.StemTimeout, 10_000L)
        assertTrue(opts is QuestionFsmState.MultiStagedOptions)
        opts as QuestionFsmState.MultiStagedOptions
        assertEquals(10_000L, opts.stemMs)
        assertEquals(10_000L, opts.stageEnteredMs)
        assertTrue(opts.selectedIndices.isEmpty())
    }

    @Test
    fun `2_4-UNIT-019 MultiStagedOptions OptionsTimeout produces Writing NOT_ANSWERED`() {
        val q = multiChoice(mode = PresentMode.STAGED, stemMs = 10_000L, optionsMs = 20_000L)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val opts = fsm.reduce(stem, QuestionEvent.StemTimeout, 10_000L)
        val timeout = fsm.reduce(opts, QuestionEvent.OptionsTimeout, 30_000L)
        assertTrue(timeout is QuestionFsmState.Writing)
        timeout as QuestionFsmState.Writing
        assertEquals("", timeout.record.answer)
        assertEquals(0, timeout.record.score)
        assertEquals(ResultStatus.NOT_ANSWERED, timeout.record.status)
        assertEquals(10_000L, timeout.record.stemMs)
    }

    @Test
    fun `2_4-UNIT-020 ToggleOption in MultiStagedStem is ignored`() {
        val q = multiChoice(mode = PresentMode.STAGED)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val result = fsm.reduce(stem, QuestionEvent.ToggleOption(1), 500L)
        assertEquals(stem, result)
    }

    @Test
    fun `2_4-BLIND-DATA-001 multi staged append called exactly once per question`() {
        val q = multiChoice(mode = PresentMode.STAGED, stemMs = 2_000L, optionsMs = 3_000L)
        val stem = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        val opts = fsm.reduce(stem, QuestionEvent.StemTimeout, 2_000L)
        assertTrue(opts !is QuestionFsmState.Writing)
        val toggled = fsm.reduce(opts, QuestionEvent.ToggleOption(1), 2_500L)
        val toggled2 = fsm.reduce(toggled, QuestionEvent.ToggleOption(3), 2_800L)
        val writing =
            fsm.reduce(toggled2, QuestionEvent.MultiOptionsSubmit(setOf(1, 3)), 4_000L)
        assertTrue(writing is QuestionFsmState.Writing)
    }

    @Test
    fun `EnterMulti staged with null stemDurationMs transitions to Errored`() {
        val q = multiChoice(mode = PresentMode.STAGED, stemMs = null)
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMulti(q), 0L)
        assertTrue(s is QuestionFsmState.Errored)
    }

    // ── Story 3.1: Memory ──

    private fun memoryQuestion(
        qid: String = "M1",
        dotsPositions: List<Int> = listOf(3, 7, 12, 19, 22, 30, 37, 44, 51, 58),
        optionsMs: Long = 30_000L,
    ): Question.Memory =
        Question.Memory(
            qid = qid,
            mode = PresentMode.ALL_IN_ONE,
            stemDurationMs = null,
            optionsDurationMs = optionsMs,
            dotsPositions = dotsPositions,
        )

    @Test
    fun `3_1-UNIT-001 EnterMemory transitions to MemoryRendering`() {
        val q = memoryQuestion()
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(q), 1_000L)
        assertTrue("expected MemoryRendering, got $s", s is QuestionFsmState.MemoryRendering)
        s as QuestionFsmState.MemoryRendering
        assertEquals(q, s.question)
    }

    @Test
    fun `3_1-UNIT-004 EnterMemory with corner indices 0 and 63`() {
        val q = memoryQuestion(dotsPositions = listOf(0, 7, 12, 19, 22, 30, 37, 44, 56, 63))
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(q), 0L)
        assertTrue(s is QuestionFsmState.MemoryRendering)
        assertEquals(0, (s as QuestionFsmState.MemoryRendering).question.dotsPositions.first())
        assertEquals(63, s.question.dotsPositions.last())
    }

    @Test
    fun `3_1-UNIT-005 EnterMemory with adjacent indices 0 and 1`() {
        val q = memoryQuestion(dotsPositions = listOf(0, 1, 12, 19, 22, 30, 37, 44, 51, 58))
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(q), 0L)
        assertTrue(s is QuestionFsmState.MemoryRendering)
    }

    @Test
    fun `3_1-BLIND-BOUNDARY-001 EnterMemory with all 4 corners`() {
        val q = memoryQuestion(dotsPositions = listOf(0, 7, 56, 63, 12, 19, 22, 30, 37, 44))
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(q), 0L)
        assertTrue(s is QuestionFsmState.MemoryRendering)
    }

    @Test
    fun `3_1-BLIND-BOUNDARY-002 EnterMemory with 10 consecutive indices`() {
        val q = memoryQuestion(dotsPositions = (0..9).toList())
        val s = fsm.reduce(QuestionFsmState.Init, QuestionEvent.EnterMemory(q), 0L)
        assertTrue(s is QuestionFsmState.MemoryRendering)
    }
}
