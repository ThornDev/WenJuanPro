package ai.wenjuanpro.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HitTestUtilTest {
    private val positions = listOf(3, 7, 12, 19, 22, 30, 37, 44, 51, 58)
    private val cellSize = 40f // pixels
    private val radius = cellSize * 0.35f // 14f

    private fun centerOf(gridIndex: Int): Pair<Float, Float> {
        val col = gridIndex % 8
        val row = gridIndex / 8
        return (col + 0.5f) * cellSize to (row + 0.5f) * cellSize
    }

    @Test
    fun `3_3-UNIT-005 hit test detects dot within radius`() {
        val (cx, cy) = centerOf(3)
        val result = HitTestUtil.hitTest(cx + 5f, cy + 5f, positions, cellSize, emptySet())
        assertEquals(3, result)
    }

    @Test
    fun `3_3-UNIT-006 hit test returns null when too far`() {
        val result = HitTestUtil.hitTest(0f, 0f, positions, cellSize, emptySet())
        // (0,0) is far from center of index 0 which is at (20,20), but index 0 is not in positions
        // The nearest position is index 3 at col=3,row=0 → center (140, 20), distance >> 14
        assertNull(result)
    }

    @Test
    fun `3_3-UNIT-007 hit test skips already selected dot`() {
        val (cx, cy) = centerOf(3)
        val result = HitTestUtil.hitTest(cx, cy, positions, cellSize, setOf(3))
        assertNull(result)
    }

    @Test
    fun `3_3-UNIT-008 hit test at exact radius boundary is hit`() {
        val (cx, cy) = centerOf(7)
        // Touch exactly at radius distance
        val result = HitTestUtil.hitTest(cx + radius, cy, positions, cellSize, emptySet())
        assertEquals(7, result)
    }
}
