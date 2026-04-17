package ai.wenjuanpro.app.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for Story 2.1 — SessionStateHolder extension (studentId StateFlow).
 *
 * The original SessionStateHolderTest covers selectConfig/clear (Story 1.4); this file adds
 * the studentId behavior for Story 2.1.
 */
class SessionStateHolderStudentIdTest {
    @Test
    fun `2_1-UNIT-028 setStudentId updates studentId StateFlow value`() {
        val holder = SessionStateHolder()
        holder.setStudentId("S001")
        assertEquals("S001", holder.studentId.value)
    }

    @Test
    fun `2_1-UNIT-029 clear nulls both selectedConfigId and studentId (BLIND-DATA-001 cascade)`() {
        val holder = SessionStateHolder()
        holder.selectConfig("cog-mem")
        holder.setStudentId("S001")
        holder.clear()
        assertNull(holder.selectedConfigId.value)
        assertNull(holder.studentId.value)
    }
}
