package ai.wenjuanpro.app.data.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultFileParserTest {

    @Test
    fun `2_5-UNIT-015 parse completedQids from valid file`() {
        val content = """
            deviceId: abc123
            studentId: S001
            configId: test
            sessionStart: 20260417-103045
            appVersion: 0.1.0
            ---
            Q1|single|all_in_one|-|24530|2|1|0|done
            Q2|multi|staged|10000|8420|1,3|1,2|1|done
            Q3|single|all_in_one|-|30000||1|0|not_answered
        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        assertEquals(setOf("Q1", "Q2", "Q3"), qids)
    }

    @Test
    fun `2_5-UNIT-016 empty file returns empty set`() {
        val content = """
            deviceId: abc123
            studentId: S001
            configId: test
            sessionStart: 20260417-103045
            appVersion: 0.1.0
            ---
        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        assertTrue("Expected empty set", qids!!.isEmpty())
    }

    @Test
    fun `2_5-UNIT-017 no separator returns null`() {
        val content = "deviceId: abc123\nstudentId: S001\n"
        assertNull(ResultFileParser.parseCompletedQids(content))
    }

    @Test
    fun `2_5-UNIT-018 malformed data line skipped`() {
        val content = """
            deviceId: abc123
            ---
            Q1|single|all_in_one|-|24530|2|1|0|done
            bad_line_with_few_fields
            Q3|single|all_in_one|-|30000||1|0|not_answered
        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        assertEquals(setOf("Q1", "Q3"), qids)
    }

    @Test
    fun `2_5-BLIND-DATA-001 trailing empty lines ignored`() {
        val content = """
            deviceId: abc123
            ---
            Q1|single|all_in_one|-|24530|2|1|0|done
            Q2|multi|staged|10000|8420|1,3|1,2|1|done


        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        assertEquals(setOf("Q1", "Q2"), qids)
    }
}
