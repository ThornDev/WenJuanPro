package ai.wenjuanpro.app.domain.fsm

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.ResultStatus
import ai.wenjuanpro.app.domain.model.StemContent
import ai.wenjuanpro.app.domain.usecase.ScoreSingleChoiceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionFsmTest {
    private lateinit var fsm: QuestionFsm

    @Before
    fun setUp() {
        fsm = QuestionFsm(score = ScoreSingleChoiceUseCase())
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
}
