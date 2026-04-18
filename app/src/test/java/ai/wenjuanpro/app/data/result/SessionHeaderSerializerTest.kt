package ai.wenjuanpro.app.data.result

import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SessionHeaderSerializerTest {

    private fun session(
        deviceId: String = "abc123",
        studentId: String = "S001",
        configId: String = "cog-mem-2026q3",
        start: LocalDateTime = LocalDateTime.of(2026, 4, 17, 10, 30, 45),
    ): Session = Session(
        studentId = studentId,
        deviceId = deviceId,
        config = Config(configId = configId, title = "Test", sourceFileName = "t.txt", questions = emptyList()),
        sessionStart = start,
        resultFileName = "${deviceId}_${studentId}_${configId}_20260417-103045.txt",
    )

    @Test
    fun `2_5-UNIT-006 standard header 5 key-value lines plus separator`() {
        val header = SessionHeaderSerializer.serialize(session(), "0.1.0")
        val expected = """
            deviceId: abc123
            studentId: S001
            configId: cog-mem-2026q3
            sessionStart: 20260417-103045
            appVersion: 0.1.0
            ---
        """.trimIndent() + "\n"
        assertEquals(expected, header)
    }

    @Test
    fun `2_5-UNIT-007 sessionStart formatted as yyyyMMdd-HHmmss`() {
        val s = session(start = LocalDateTime.of(2026, 1, 5, 9, 5, 3))
        val header = SessionHeaderSerializer.serialize(s, "1.0.0")
        assertTrue("Expected 20260105-090503", header.contains("sessionStart: 20260105-090503"))
    }

    @Test
    fun `2_5-BLIND-BOUNDARY-003 special characters in configId`() {
        val s = session(configId = "cog-mem_2026-q3")
        val header = SessionHeaderSerializer.serialize(s, "1.0.0")
        assertTrue("ConfigId with hyphens/underscores", header.contains("configId: cog-mem_2026-q3"))
    }
}
