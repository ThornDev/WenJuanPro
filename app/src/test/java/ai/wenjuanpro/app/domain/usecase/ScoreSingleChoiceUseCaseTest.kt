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

class ScoreSingleChoiceUseCaseTest {
    private lateinit var useCase: ScoreSingleChoiceUseCase

    @Before
    fun setUp() {
        useCase = ScoreSingleChoiceUseCase()
    }

    private fun question(
        mode: PresentMode = PresentMode.ALL_IN_ONE,
        scores: List<Int> = listOf(10, 5, 0, 0),
        options: List<OptionContent> =
            listOf(
                OptionContent.Text("A"),
                OptionContent.Text("B"),
                OptionContent.Text("C"),
                OptionContent.Text("D"),
            ),
        correctIndex: Int = 1,
        stemDurationMs: Long? = if (mode == PresentMode.STAGED) 10_000L else null,
        optionsDurationMs: Long = 30_000L,
        qid: String = "Q1",
    ): Question.SingleChoice =
        Question.SingleChoice(
            qid = qid,
            mode = mode,
            stemDurationMs = stemDurationMs,
            optionsDurationMs = optionsDurationMs,
            stem = StemContent.Text("stem"),
            options = options,
            correctIndex = correctIndex,
            scores = scores,
        )

    @Test
    fun `2_3-UNIT-001 hit option 1 returns scores 0`() {
        val record =
            useCase(
                question = question(scores = listOf(10, 5, 0, 0)),
                answer = 1,
                stemMs = null,
                optionsMs = 2_000L,
            )
        assertEquals("1", record.answer)
        assertEquals(10, record.score)
        assertEquals(ResultStatus.DONE, record.status)
        assertEquals(QuestionType.SINGLE, record.type)
        assertEquals(PresentMode.ALL_IN_ONE, record.mode)
        assertEquals(2_000L, record.optionsMs)
        assertNull(record.stemMs)
    }

    @Test
    fun `2_3-UNIT-002 hit last option returns scores last`() {
        val record =
            useCase(
                question = question(scores = listOf(0, 0, 0, 8)),
                answer = 4,
                stemMs = null,
                optionsMs = 1_000L,
            )
        assertEquals("4", record.answer)
        assertEquals(8, record.score)
        assertEquals(ResultStatus.DONE, record.status)
    }

    @Test
    fun `2_3-UNIT-003 null answer returns NOT_ANSWERED with score 0`() {
        val record =
            useCase(
                question = question(),
                answer = null,
                stemMs = null,
                optionsMs = 30_000L,
            )
        assertEquals("", record.answer)
        assertEquals(0, record.score)
        assertEquals(ResultStatus.NOT_ANSWERED, record.status)
        assertEquals(30_000L, record.optionsMs)
    }

    @Test
    fun `2_3-BLIND-DATA-002 all_in_one ResultRecord stemMs is null not zero`() {
        val record =
            useCase(
                question = question(mode = PresentMode.ALL_IN_ONE),
                answer = 1,
                stemMs = 12_345L,
                optionsMs = 4_000L,
            )
        assertNull(
            "all_in_one mode must drop stemMs into null even when caller passes a value",
            record.stemMs,
        )
    }

    @Test
    fun `2_3-BLIND-DATA-003 staged ResultRecord stemMs equals stemDurationMs exactly`() {
        val q = question(mode = PresentMode.STAGED, stemDurationMs = 10_000L)
        val record =
            useCase(
                question = q,
                answer = 1,
                stemMs = q.stemDurationMs,
                optionsMs = 8_000L,
            )
        assertEquals(10_000L, record.stemMs)
    }

    @Test
    fun `2_3-BLIND-BOUNDARY-004 out-of-range answer throws IllegalStateException`() {
        assertThrows(IllegalStateException::class.java) {
            useCase(
                question = question(),
                answer = 99,
                stemMs = null,
                optionsMs = 1_000L,
            )
        }
    }

    @Test
    fun `2_3-BLIND-BOUNDARY-005 scores size mismatch throws IllegalStateException`() {
        val q =
            question(
                scores = listOf(1, 2),
                options =
                    listOf(
                        OptionContent.Text("A"),
                        OptionContent.Text("B"),
                        OptionContent.Text("C"),
                        OptionContent.Text("D"),
                    ),
            )
        assertThrows(IllegalStateException::class.java) {
            useCase(question = q, answer = 1, stemMs = null, optionsMs = 1_000L)
        }
    }
}
