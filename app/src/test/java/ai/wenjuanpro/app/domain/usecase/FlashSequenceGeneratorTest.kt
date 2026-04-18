package ai.wenjuanpro.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashSequenceGeneratorTest {
    private val generator = FlashSequenceGenerator()
    private val positions = listOf(3, 7, 12, 19, 22, 30, 37, 44, 51, 58)

    @Test
    fun `3_2-UNIT-001 generate returns all 10 elements no duplicates`() {
        val result = generator.generate(positions)
        assertEquals(10, result.size)
        assertEquals(positions.toSet(), result.toSet())
    }

    @Test
    fun `3_2-UNIT-002 generate produces different order on multiple calls`() {
        val results = (1..20).map { generator.generate(positions) }
        val distinct = results.distinct()
        assertTrue(
            "Expected at least 2 distinct orderings in 20 calls, got ${distinct.size}",
            distinct.size >= 2,
        )
    }

    @Test
    fun `3_2-BLIND-BOUNDARY-001 generate with exactly 10 elements returns exactly 10`() {
        val result = generator.generate(positions)
        assertEquals(positions.size, result.size)
    }
}
