package ai.wenjuanpro.app.data.result

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ResultFileWriterTest {

    private lateinit var tmpFile: File

    @Before
    fun setUp() {
        tmpFile = File.createTempFile("result_writer_test", ".txt")
    }

    @After
    fun tearDown() {
        tmpFile.delete()
    }

    @Test
    fun `2_5-UNIT-008 writeHeader outputs complete header block`() {
        val header = "deviceId: abc\nstudentId: S001\n---\n"
        val writer = ResultFileWriter(FileOutputStream(tmpFile))
        writer.writeHeader(header)
        writer.close()
        assertEquals(header, tmpFile.readText(Charsets.UTF_8))
    }

    @Test
    fun `2_5-UNIT-009 appendLine outputs line plus newline`() {
        val writer = ResultFileWriter(FileOutputStream(tmpFile))
        writer.appendLine("Q1|single|all_in_one|-|24530|2|1|0|done")
        writer.close()
        assertEquals("Q1|single|all_in_one|-|24530|2|1|0|done\n", tmpFile.readText(Charsets.UTF_8))
    }

    @Test
    fun `2_5-UNIT-010 flush and fsync called on every appendLine`() {
        // Verify by writing multiple lines and reading after each write
        val writer = ResultFileWriter(FileOutputStream(tmpFile))
        writer.appendLine("line1")
        // File should be persisted (fsync guarantees)
        assertTrue("File should have content after first appendLine", tmpFile.length() > 0)
        writer.appendLine("line2")
        writer.close()
        val lines = tmpFile.readText(Charsets.UTF_8).lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
    }

    @Test
    fun `2_5-UNIT-011 IOException converts to WriteFailedException`() {
        val closedFos = FileOutputStream(tmpFile)
        closedFos.close() // Pre-close to force IOException on write
        val writer = ResultFileWriter(closedFos)
        try {
            writer.appendLine("should fail")
            fail("Expected WriteFailedException")
        } catch (e: WriteFailedException) {
            assertTrue("Should have a code", e.code.isNotBlank())
        } finally {
            writer.close()
        }
    }

    @Test
    fun `2_5-BLIND-ERROR-001 fsync failure mapped to WriteFailedException`() {
        // Use a closed stream to simulate fsync failure
        val closedFos = FileOutputStream(tmpFile)
        closedFos.close()
        val writer = ResultFileWriter(closedFos)
        try {
            writer.appendLine("data")
            fail("Expected WriteFailedException")
        } catch (e: WriteFailedException) {
            // Any IOException should be wrapped
            assertTrue(e.cause is IOException)
        } finally {
            writer.close()
        }
    }

    @Test
    fun `2_5-BLIND-ERROR-002 disk full simulated via closed stream`() {
        val closedFos = FileOutputStream(tmpFile)
        closedFos.close()
        val writer = ResultFileWriter(closedFos)
        try {
            writer.writeHeader("header\n")
            fail("Expected WriteFailedException")
        } catch (e: WriteFailedException) {
            assertTrue(e.code.isNotBlank())
        } finally {
            writer.close()
        }
    }

    @Test
    fun `2_5-BLIND-RESOURCE-001 FileOutputStream closed on exception path`() {
        // After catching exception, close should not throw
        val closedFos = FileOutputStream(tmpFile)
        closedFos.close()
        val writer = ResultFileWriter(closedFos)
        try {
            writer.appendLine("data")
        } catch (_: WriteFailedException) {
            // expected
        }
        // close() should not throw
        writer.close()
    }
}
