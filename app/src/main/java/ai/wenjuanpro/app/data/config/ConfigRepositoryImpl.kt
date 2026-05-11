package ai.wenjuanpro.app.data.config

import ai.wenjuanpro.app.core.concurrency.IoDispatcher
import ai.wenjuanpro.app.core.io.FileSystem
import ai.wenjuanpro.app.data.parser.ConfigParser
import ai.wenjuanpro.app.data.parser.ParseError
import ai.wenjuanpro.app.data.parser.ParseErrorCode
import ai.wenjuanpro.app.data.parser.ParseResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl
    @Inject
    constructor(
        private val fileSystem: FileSystem,
        private val parser: ConfigParser,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ConfigRepository {
        override suspend fun ensureConfigDir(): Boolean =
            withContext(ioDispatcher) {
                if (fileSystem.exists(CONFIG_DIR)) return@withContext true
                val ok = fileSystem.mkdirs(CONFIG_DIR)
                if (!ok) Timber.w("ensureConfigDir mkdirs failed")
                ok
            }

        override suspend fun loadAll(): List<ConfigLoadResult> =
            withContext(ioDispatcher) {
                // We deliberately load a single fixed file rather than every
                // .txt under config/, because the field workflow ships exactly
                // one questionnaire at a time and "pick the first valid one"
                // turned out to be a reliable way to drift onto an old draft
                // left in the directory. Researchers wanting a different
                // active config just rename their file to CONFIG_FILE_NAME.
                val path = CONFIG_DIR + CONFIG_FILE_NAME
                if (!fileSystem.exists(path)) return@withContext emptyList()
                val result =
                    try {
                        val bytes = fileSystem.readBytes(path)
                        when (val pr = parser.parse(CONFIG_FILE_NAME, bytes)) {
                            is ParseResult.Success -> ConfigLoadResult.Valid(pr.config)
                            is ParseResult.Failure ->
                                ConfigLoadResult.Invalid(CONFIG_FILE_NAME, pr.errors)
                        }
                    } catch (e: IOException) {
                        Timber.w("readBytes failed for active config file; marking invalid")
                        ConfigLoadResult.Invalid(
                            fileName = CONFIG_FILE_NAME,
                            errors =
                                listOf(
                                    ParseError(
                                        line = 0,
                                        field = null,
                                        code = ParseErrorCode.CONFIG_FIELD_INVALID,
                                        message = "$CONFIG_FILE_NAME: 读取文件失败（IO 异常）",
                                    ),
                                ),
                        )
                    }
                listOf(result)
            }

        companion object {
            private const val CONFIG_DIR = "/sdcard/WenJuanPro/config/"
            /**
             * Single source of truth for the active questionnaire filename.
             * Edit here when the field protocol changes; only one file under
             * /sdcard/WenJuanPro/config/ is ever read.
             */
            const val CONFIG_FILE_NAME: String = "quiz.txt"
        }
    }
