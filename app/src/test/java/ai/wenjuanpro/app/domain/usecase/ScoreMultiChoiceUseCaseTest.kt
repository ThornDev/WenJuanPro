package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultStatus
import ai.wenjuanpro.app.domain.model.StemContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ScoreMultiChoiceUseCaseTest {
    private lateinit var useCase: ScoreMultiChoiceUseCase

    @Before
    fun setUp() {
        useCase = ScoreMultiChoiceUseCase()
    }

    private fun multi(
        mode: PresentMode = PresentMode.ALL_IN_ONE,
        scores: List<Int> = listOf(10, 5, 3, 2),
        options: List<OptionContent> =
            listOf(
                OptionContent.Text("A"),
                OptionContent.Text("B"),
                OptionContent.Text("C"),
                OptionContent.Text("D"),
            ),
        correctIndices: Set<Int> = setOf(1, 2),
        stemDurationMs: Long? = if (mode == PresentMode.STAGED) 10_000L else null,
        optionsDurationMs: Long = 30_000L,
        qid: String = "Q1",
    ): Question.MultiChoice =
        Question.MultiChoice(
            qid = qid,
            mode = mode,
            stemDurationMs = stemDurationMs,
            optionsDurationMs = optionsDurationMs,
            stem = StemContent.Text("stem"),
            options = options,
            correctIndices = correctIndices,
            scores = scores,
        )

    @Test
    fun `2_4-UNIT-001 single selection returns scores_i`() {
        val record =
            useCase(
                question = multi(scores = listOf(10, 5, 0, 0)),
                selectedIndices = setOf(2),
                stemMs = null,
                optionsMs = 2_000L,
            )
        assertEquals("2", record.answer)
        assertEquals(5, record.score)
        assertEquals(ResultStatus.DONE, record.status)
        assertEquals(QuestionType.MULTI, record.type)
        assertEquals(PresentMode.ALL_IN_ONE, record.mode)
        assertNull(record.stemMs)
        assertEquals(2_000L, record.optionsMs)
    }

    @Test
    fun `2_4-UNIT-002 multi selection sums scores`() {
        val record =
            useCase(
                question = multi(scores = listOf(10, 5, 3, 2)),
                selectedIndices = setOf(1, 3, 4),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("1,3,4", record.answer)
        assertEquals(15, record.score)
        assertEquals(ResultStatus.DONE, record.status)
    }

    @Test
    fun `2_4-UNIT-003 empty or null selection returns NOT_ANSWERED`() {
        val nullRecord =
            useCase(
                question = multi(),
                selectedIndices = null,
                stemMs = null,
                optionsMs = 30_000L,
            )
        assertEquals("", nullRecord.answer)
        assertEquals(0, nullRecord.score)
        assertEquals(ResultStatus.NOT_ANSWERED, nullRecord.status)
        assertEquals(30_000L, nullRecord.optionsMs)

        val emptyRecord =
            useCase(
                question = multi(),
                selectedIndices = emptySet(),
                stemMs = null,
                optionsMs = 30_000L,
            )
        assertEquals("", emptyRecord.answer)
        assertEquals(ResultStatus.NOT_ANSWERED, emptyRecord.status)
    }

    @Test
    fun `2_4-UNIT-004 unordered input yields ascending csv answer`() {
        val record =
            useCase(
                question = multi(),
                selectedIndices = setOf(4, 1, 3),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("1,3,4", record.answer)
    }

    @Test
    fun `2_4-UNIT-005 correct field is ascending csv of correctIndices`() {
        val record =
            useCase(
                question = multi(correctIndices = setOf(2, 1)),
                selectedIndices = setOf(1),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("1,2", record.correct)

        val empty =
            useCase(
                question = multi(correctIndices = emptySet()),
                selectedIndices = setOf(1),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("", empty.correct)
    }

    @Test
    fun `2_4-UNIT-022 all_in_one drops stemMs to null, staged preserves it`() {
        val all =
            useCase(
                question = multi(mode = PresentMode.ALL_IN_ONE),
                selectedIndices = setOf(1),
                stemMs = 12_345L,
                optionsMs = 4_000L,
            )
        assertNull(all.stemMs)

        val stagedQ = multi(mode = PresentMode.STAGED, stemDurationMs = 10_000L)
        val staged =
            useCase(
                question = stagedQ,
                selectedIndices = setOf(1),
                stemMs = stagedQ.stemDurationMs,
                optionsMs = 8_000L,
            )
        assertEquals(10_000L, staged.stemMs)
    }

    @Test
    fun `2_4-BLIND-BOUNDARY-001 null selectedIndices returns NOT_ANSWERED without NPE`() {
        val record =
            useCase(
                question = multi(),
                selectedIndices = null,
                stemMs = null,
                optionsMs = 30_000L,
            )
        assertEquals(ResultStatus.NOT_ANSWERED, record.status)
        assertEquals(0, record.score)
        assertEquals("", record.answer)
    }

    @Test
    fun `2_4-BLIND-BOUNDARY-003 all-zero scores with empty correctIndices yields correct empty`() {
        val record =
            useCase(
                question =
                    multi(
                        scores = listOf(0, 0, 0, 0),
                        correctIndices = emptySet(),
                    ),
                selectedIndices = setOf(1, 2),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("1,2", record.answer)
        assertEquals("", record.correct)
        assertEquals(0, record.score)
        assertEquals(ResultStatus.DONE, record.status)
    }

    @Test
    fun `2_4-BLIND-BOUNDARY-006 out-of-range selection throws IllegalStateException`() {
        assertThrows(IllegalStateException::class.java) {
            useCase(
                question = multi(),
                selectedIndices = setOf(99),
                stemMs = null,
                optionsMs = 1_000L,
            )
        }
    }

    @Test
    fun `2_4-BLIND-DATA-002 answer csv is ascending regardless of Set iteration order`() {
        val a =
            useCase(
                question = multi(),
                selectedIndices = linkedSetOf(3, 1),
                stemMs = null,
                optionsMs = 1_000L,
            )
        val b =
            useCase(
                question = multi(),
                selectedIndices = sortedSetOf(1, 3),
                stemMs = null,
                optionsMs = 1_000L,
            )
        val c =
            useCase(
                question = multi(),
                selectedIndices = setOf(1, 3),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("1,3", a.answer)
        assertEquals("1,3", b.answer)
        assertEquals("1,3", c.answer)
    }

    @Test
    fun `2_4-BLIND-BOUNDARY-002 options size one allows single toggle selection`() {
        val record =
            useCase(
                question =
                    multi(
                        options = listOf(OptionContent.Text("Only")),
                        scores = listOf(7),
                        correctIndices = setOf(1),
                    ),
                selectedIndices = setOf(1),
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("1", record.answer)
        assertEquals("1", record.correct)
        assertEquals(7, record.score)
    }
}
