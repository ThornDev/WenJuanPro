package ai.wenjuanpro.app.domain.session

import ai.wenjuanpro.app.domain.model.Config
import ai.wenjuanpro.app.domain.model.OptionContent
import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.Session
import ai.wenjuanpro.app.domain.model.StemContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SessionStateHolderSessionTest {
    private fun config(): Config =
        Config(
            configId = "cfg-1",
            title = "T",
            sourceFileName = "cfg-1.txt",
            questions =
                listOf(
                    Question.SingleChoice(
                        qid = "Q1",
                        mode = PresentMode.ALL_IN_ONE,
                        stemDurationMs = null,
                        optionsDurationMs = 30_000L,
                        stem = StemContent.Text("s"),
                        options = listOf(OptionContent.Text("A"), OptionContent.Text("B")),
                        correctIndex = 1,
                        scores = listOf(1, 0),
                    ),
                ),
        )

    @Test
    fun `2_3-UNIT-EXT-001 openSession populates currentSession + selectedConfig + studentId`() {
        val holder = SessionStateHolder()
        val session =
            Session(
                studentId = "U1",
                deviceId = "DEV",
                config = config(),
                sessionStart = LocalDateTime.of(2026, 4, 18, 10, 0, 0),
                resultFileName = "DEV_U1_cfg-1_20260418-100000.txt",
            )
        holder.openSession(session)
        assertEquals(session, holder.currentSession.value)
        assertEquals(config(), holder.selectedConfig.value)
        assertEquals("cfg-1", holder.selectedConfigId.value)
        assertEquals("U1", holder.studentId.value)
    }

    @Test
    fun `2_3-UNIT-EXT-002 advanceCursor increments cursor and adds qid`() {
        val holder = SessionStateHolder()
        val session =
            Session(
                studentId = "U1",
                deviceId = "DEV",
                config = config(),
                sessionStart = LocalDateTime.of(2026, 4, 18, 10, 0, 0),
                resultFileName = "DEV_U1_cfg-1_20260418-100000.txt",
            )
        holder.openSession(session)
        holder.advanceCursor("Q1")
        val s = holder.currentSession.value!!
        assertEquals(1, s.cursor)
        assertTrue(s.completedQids.contains("Q1"))
    }

    @Test
    fun `2_3-UNIT-EXT-003 setSelectedConfig syncs configId`() {
        val holder = SessionStateHolder()
        holder.setSelectedConfig(config())
        assertEquals("cfg-1", holder.selectedConfigId.value)
    }

    @Test
    fun `clear nulls currentSession and selectedConfig too`() {
        val holder = SessionStateHolder()
        holder.setSelectedConfig(config())
        holder.openSession(
            Session(
                studentId = "U1",
                deviceId = "DEV",
                config = config(),
                sessionStart = LocalDateTime.now(),
                resultFileName = "x.txt",
            ),
        )
        holder.clear()
        assertNull(holder.currentSession.value)
        assertNull(holder.selectedConfig.value)
        assertNull(holder.selectedConfigId.value)
        assertNull(holder.studentId.value)
    }
}
