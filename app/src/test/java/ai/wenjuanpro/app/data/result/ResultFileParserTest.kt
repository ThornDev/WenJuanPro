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
        assertEquals(setOf("Q1", "Q2"), qids) // Q3 is not_answered → excluded
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
        assertEquals(setOf("Q1"), qids) // Q3 is not_answered → excluded
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

    // Story 4.2: status-aware filtering

    @Test
    fun `4_2 parseCompletedQids includes terminal statuses but skips error`() {
        val content = """
            deviceId: abc123
            ---
            Q1|single|all_in_one|-|24530|2|1|0|done
            Q2|multi|staged|10000|8420||1,2|0|not_answered
            Q3|memory|all_in_one|-|42300|3,7|3,7,12|2|done
            Q4|single|all_in_one|-|30000||1|0|error
        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        // not_answered is a terminal state (timer expired) so it counts;
        // error means the write itself blew up and the qid can be retried.
        assertEquals(setOf("Q1", "Q2", "Q3"), qids)
    }

    @Test
    fun `4_2 parseQidsByStatus returns all qids with last status`() {
        val content = """
            deviceId: abc123
            ---
            Q1|single|all_in_one|-|24530|2|1|0|done
            Q2|multi|staged|10000|8420||1,2|0|not_answered
            Q1|single|all_in_one|-|25000|1|1|10|done
        """.trimIndent()
        val statusMap = ResultFileParser.parseQidsByStatus(content)!!
        assertEquals("done", statusMap["Q1"])       // last line wins
        assertEquals("not_answered", statusMap["Q2"])
    }

    // Story 4.3: partial status is also a terminal state

    @Test
    fun `4_3 partial status counts as completed`() {
        val content = """
            deviceId: abc123
            ---
            Q1|single|all_in_one|-|24530|2|1|0|done
            Q2|single|staged|10000|-||1|0|partial
        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        assertEquals(setOf("Q1", "Q2"), qids)
    }

    @Test
    fun `4_3 redo overwrites partial with done`() {
        val content = """
            deviceId: abc123
            ---
            Q2|single|staged|10000|-||1|0|partial
            Q2|single|staged|10000|25000|2|1|5|done
        """.trimIndent()
        val qids = ResultFileParser.parseCompletedQids(content)
        assertEquals(setOf("Q2"), qids) // last line is done → included
    }
}
