package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreMemoryUseCaseTest {
    private val useCase = ScoreMemoryUseCase()
    private val question = Question.Memory(
        qid = "M1",
        mode = PresentMode.ALL_IN_ONE,
        stemDurationMs = null,
        optionsDurationMs = 60_000L,
        dotsPositions = listOf(3, 7, 12, 19, 22, 30, 37, 44, 51, 58),
    )
    private val expected = listOf(7, 3, 44, 19, 58, 12, 30, 22, 37, 51)

    @Test
    fun `3_3-UNIT-001 perfect match score 10 status DONE`() {
        val result = useCase(question, answer = expected, expected = expected, optionsMs = 42_000L)
        assertEquals(10, result.score)
        assertEquals(ResultStatus.DONE, result.status)
        assertEquals(QuestionType.MEMORY, result.type)
    }

    @Test
    fun `3_3-UNIT-002 partial match score equals prefix count`() {
        val answer = listOf(7, 3, 12) // first 2 match, 3rd doesn't
        val result = useCase(question, answer = answer, expected = expected, optionsMs = 50_000L)
        assertEquals(2, result.score)
        assertEquals(ResultStatus.DONE, result.status)
    }

    @Test
    fun `3_3-UNIT-003 all wrong score 0 status DONE`() {
        val answer = listOf(58, 51, 37) // first doesn't match
        val result = useCase(question, answer = answer, expected = expected, optionsMs = 60_000L)
        assertEquals(0, result.score)
        assertEquals(ResultStatus.DONE, result.status)
    }

    @Test
    fun `3_3-UNIT-004 empty answer status NOT_ANSWERED`() {
        val result = useCase(question, answer = emptyList(), expected = expected, optionsMs = 60_000L)
        assertEquals(0, result.score)
        assertEquals(ResultStatus.NOT_ANSWERED, result.status)
        assertEquals("", result.answer)
    }
}
