package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultRecordSerializerTest {

    @Test
    fun `2_5-UNIT-001 single all_in_one serialization`() {
        val record = ResultRecord(
            qid = "Q1", type = QuestionType.SINGLE, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 24530, answer = "2", correct = "1",
            score = 0, status = ResultStatus.DONE,
        )
        assertEquals("Q1|single|all_in_one|-|24530|2|1|0|done", ResultRecordSerializer.serialize(record))
    }

    @Test
    fun `2_5-UNIT-002 multi staged serialization with stemMs`() {
        val record = ResultRecord(
            qid = "Q2", type = QuestionType.MULTI, mode = PresentMode.STAGED,
            stemMs = 10000, optionsMs = 8420, answer = "1,3", correct = "1,2",
            score = 1, status = ResultStatus.DONE,
        )
        assertEquals("Q2|multi|staged|10000|8420|1,3|1,2|1|done", ResultRecordSerializer.serialize(record))
    }

    @Test
    fun `2_5-UNIT-003 not_answered with empty answer`() {
        val record = ResultRecord(
            qid = "Q3", type = QuestionType.SINGLE, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 30000, answer = "", correct = "1",
            score = 0, status = ResultStatus.NOT_ANSWERED,
        )
        assertEquals("Q3|single|all_in_one|-|30000||1|0|not_answered", ResultRecordSerializer.serialize(record))
    }

    @Test
    fun `2_5-UNIT-004 memory type serialization`() {
        val record = ResultRecord(
            qid = "Q4", type = QuestionType.MEMORY, mode = PresentMode.STAGED,
            stemMs = 5000, optionsMs = 12000, answer = "3,7,22,12", correct = "3,7,22,12,45",
            score = 4, status = ResultStatus.DONE,
        )
        assertEquals("Q4|memory|staged|5000|12000|3,7,22,12|3,7,22,12,45|4|done", ResultRecordSerializer.serialize(record))
    }

    @Test
    fun `2_5-UNIT-005 partial status serialization`() {
        val record = ResultRecord(
            qid = "Q5", type = QuestionType.MULTI, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 20000, answer = "1", correct = "1,2",
            score = 1, status = ResultStatus.PARTIAL,
        )
        val line = ResultRecordSerializer.serialize(record)
        assertEquals("Q5|multi|all_in_one|-|20000|1|1,2|1|partial", line)
    }

    @Test
    fun `2_5-BLIND-BOUNDARY-001 empty answer string serialization`() {
        val record = ResultRecord(
            qid = "Q1", type = QuestionType.SINGLE, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 30000, answer = "", correct = "1",
            score = 0, status = ResultStatus.NOT_ANSWERED,
        )
        val line = ResultRecordSerializer.serialize(record)
        // Two consecutive pipes for empty answer
        assert(line.contains("||")) { "Expected empty answer field, got: $line" }
    }

    @Test
    fun `2_5-BLIND-BOUNDARY-002 very long answer string`() {
        val longAnswer = (1..100).joinToString(",")
        val record = ResultRecord(
            qid = "Q1", type = QuestionType.MEMORY, mode = PresentMode.STAGED,
            stemMs = 5000, optionsMs = 60000, answer = longAnswer, correct = longAnswer,
            score = 100, status = ResultStatus.DONE,
        )
        val line = ResultRecordSerializer.serialize(record)
        assert(line.contains(longAnswer)) { "Long answer truncated" }
    }

    // Story 3.4: Memory type serialization

    @Test
    fun `3_4 memory type serialization format`() {
        val record = ResultRecord(
            qid = "Q3", type = QuestionType.MEMORY, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 42300,
            answer = "3,7,12,19,22,30,37,44,51,58",
            correct = "3,7,12,19,22,30,37,44,51,58",
            score = 10, status = ResultStatus.DONE,
        )
        assertEquals(
            "Q3|memory|all_in_one|-|42300|3,7,12,19,22,30,37,44,51,58|3,7,12,19,22,30,37,44,51,58|10|done",
            ResultRecordSerializer.serialize(record),
        )
    }

    @Test
    fun `3_4 memory partial answer serialization`() {
        val record = ResultRecord(
            qid = "Q3", type = QuestionType.MEMORY, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 60000,
            answer = "3,7",
            correct = "3,7,12,19,22,30,37,44,51,58",
            score = 2, status = ResultStatus.DONE,
        )
        assertEquals(
            "Q3|memory|all_in_one|-|60000|3,7|3,7,12,19,22,30,37,44,51,58|2|done",
            ResultRecordSerializer.serialize(record),
        )
    }

    @Test
    fun `3_4 memory not answered serialization`() {
        val record = ResultRecord(
            qid = "Q3", type = QuestionType.MEMORY, mode = PresentMode.ALL_IN_ONE,
            stemMs = null, optionsMs = 60000,
            answer = "",
            correct = "3,7,12,19,22,30,37,44,51,58",
            score = 0, status = ResultStatus.NOT_ANSWERED,
        )
        assertEquals(
            "Q3|memory|all_in_one|-|60000||3,7,12,19,22,30,37,44,51,58|0|not_answered",
            ResultRecordSerializer.serialize(record),
        )
    }
}
