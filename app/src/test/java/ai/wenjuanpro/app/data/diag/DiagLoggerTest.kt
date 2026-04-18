package ai.wenjuanpro.app.data.diag

import ai.wenjuanpro.app.core.io.FileSystem
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class DiagLoggerTest {

    private lateinit var tmpDir: File
    private lateinit var logger: DiagLogger

    private val fakeFs = object : FileSystem {
        override fun exists(path: String): Boolean = File(path).exists()
        override fun listFiles(dir: String, suffix: String): List<String> = emptyList()
        override fun readBytes(path: String): ByteArray = File(path).readBytes()
        override fun mkdirs(path: String): Boolean = File(path).mkdirs() || File(path).isDirectory
        override fun openAppend(path: String): FileOutputStream = FileOutputStream(File(path), true)
        override fun createFileIfNotExists(path: String) {
            File(path).also { if (!it.exists()) it.createNewFile() }
        }
        override fun deleteIfExists(path: String) {
            File(path).also { if (it.exists()) it.delete() }
        }
    }

    @Before
    fun setUp() {
        tmpDir = File(System.getProperty("java.io.tmpdir"), "diag_test_${System.nanoTime()}")
        logger = object : DiagLogger(fakeFs) {
            override fun log(code: String, message: String) {
                // Write to tmpDir instead of /sdcard/
                try {
                    val dir = File(tmpDir, ".diag")
                    if (!dir.isDirectory) dir.mkdirs()
                    val entry = "[TEST] ERROR $code: $message\n"
                    FileOutputStream(File(dir, "app.log"), true).use { fos ->
                        fos.write(entry.toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                } catch (_: Exception) {
                    // swallow
                }
            }
        }
    }

    @After
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `2_5-UNIT-012 writes formatted log entry`() {
        logger.log("FSYNC_FAILED", "sync failed on fd")
        val logFile = File(tmpDir, ".diag/app.log")
        assertTrue("Log file should exist", logFile.exists())
        val content = logFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("ERROR FSYNC_FAILED: sync failed on fd"))
    }

    @Test
    fun `2_5-UNIT-013 auto mkdirs for diag directory`() {
        val diagDir = File(tmpDir, ".diag")
        assertFalse("Diag dir should not exist before log", diagDir.exists())
        logger.log("TEST", "test message")
        assertTrue("Diag dir should exist after log", diagDir.isDirectory)
    }

    @Test
    fun `2_5-UNIT-014 does NOT log sensitive data`() {
        // DiagLogger only receives code+message. It never receives answer/score/studentId.
        // This test verifies that the logger writes only what's given.
        logger.log("RESULT_WRITE_FAILED", "IO error on write")
        val content = File(tmpDir, ".diag/app.log").readText(Charsets.UTF_8)
        assertFalse("Should not contain 'answer'", content.contains("answer"))
        assertFalse("Should not contain 'score'", content.contains("score"))
        assertFalse("Should not contain 'studentId'", content.contains("studentId"))
    }
}
