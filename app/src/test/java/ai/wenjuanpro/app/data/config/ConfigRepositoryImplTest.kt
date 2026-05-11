package ai.wenjuanpro.app.data.config

import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.data.parser.ConfigParser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/**
 * ConfigRepositoryImpl now loads exactly one fixed file
 * (`/sdcard/WenJuanPro/config/quiz.txt`). Tests cover that single-file
 * contract: present-and-valid, present-but-corrupt, missing, IO failure,
 * and concurrency. Other .txt files in the directory are ignored.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConfigRepositoryImplTest {
    private val configDir = "/sdcard/WenJuanPro/config/"
    private val activePath = configDir + ConfigRepositoryImpl.CONFIG_FILE_NAME

    private fun validBody(configId: String): ByteArray =
        """
        # configId: $configId
        # title: T
        [Q1]
        type: single
        mode: all_in_one
        durationMs: 30000
        stem: A
        options: a|b
        correct: 1
        score: 0|0
        """.trimIndent().toByteArray(Charsets.UTF_8)

    private fun badBody(): ByteArray =
        """
        # configId: bad
        [Q1]
        type: single
        """.trimIndent().toByteArray(Charsets.UTF_8)

    /**
     * Build a FileSystem mock where `quiz.txt` either exists with the given
     * body, throws on read, or does not exist at all. loadAll only ever
     * touches the single active path so we keep the stubbing minimal.
     */
    private fun fileSystem(
        body: ByteArray? = null,
        readThrows: Boolean = false,
    ): FileSystem {
        val fs = mockk<FileSystem>()
        every { fs.exists(activePath) } returns (body != null || readThrows)
        if (readThrows) {
            every { fs.readBytes(activePath) } throws IOException("boom")
        } else if (body != null) {
            every { fs.readBytes(activePath) } returns body
        }
        return fs
    }

    private fun repo(fs: FileSystem, dispatcher: CoroutineDispatcher): ConfigRepositoryImpl =
        ConfigRepositoryImpl(fs, ConfigParser(fs), dispatcher)

    @Test
    fun `loadAll returns the single valid config when quiz file is well-formed`() =
        runTest {
            val fs = fileSystem(body = validBody("ok-cfg"))
            val results = repo(fs, StandardTestDispatcher(testScheduler)).loadAll()
            assertEquals(1, results.size)
            val valid = results.first() as ConfigLoadResult.Valid
            assertEquals("ok-cfg", valid.config.configId)
        }

    @Test
    fun `loadAll returns Invalid when the quiz file fails to parse`() =
        runTest {
            val fs = fileSystem(body = badBody())
            val results = repo(fs, StandardTestDispatcher(testScheduler)).loadAll()
            assertEquals(1, results.size)
            val invalid = results.first() as ConfigLoadResult.Invalid
            assertEquals(ConfigRepositoryImpl.CONFIG_FILE_NAME, invalid.fileName)
            assertTrue(invalid.errors.isNotEmpty())
        }

    @Test
    fun `loadAll returns empty list when quiz file is missing`() =
        runTest {
            val fs = fileSystem(body = null)
            val results = repo(fs, StandardTestDispatcher(testScheduler)).loadAll()
            assertTrue(results.isEmpty())
        }

    @Test
    fun `loadAll surfaces IOException as a single Invalid entry without crashing`() =
        runTest {
            val fs = fileSystem(readThrows = true)
            val results = repo(fs, StandardTestDispatcher(testScheduler)).loadAll()
            assertEquals(1, results.size)
            val invalid = results.first() as ConfigLoadResult.Invalid
            assertEquals(ConfigRepositoryImpl.CONFIG_FILE_NAME, invalid.fileName)
            assertTrue(invalid.errors.isNotEmpty())
        }

    @Test
    fun `loadAll executes on the injected IoDispatcher`() =
        runTest {
            val recorder = RecordingDispatcher(StandardTestDispatcher(testScheduler))
            val fs = fileSystem(body = validBody("ok-cfg"))
            repo(fs, recorder).loadAll()
            assertTrue(
                "withContext should dispatch through injected IoDispatcher",
                recorder.dispatchCount > 0,
            )
        }

    @Test
    fun `concurrent loadAll calls return consistent results`() =
        runTest {
            val fs = fileSystem(body = validBody("ok-cfg"))
            val r = repo(fs, StandardTestDispatcher(testScheduler))
            val results = listOf(async { r.loadAll() }, async { r.loadAll() }, async { r.loadAll() }).awaitAll()
            assertEquals(results[0], results[1])
            assertEquals(results[1], results[2])
        }

    private class RecordingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
        var dispatchCount = 0
            private set

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatchCount++
            delegate.dispatch(context, block)
        }

        override fun isDispatchNeeded(context: CoroutineContext): Boolean = true
    }
}
