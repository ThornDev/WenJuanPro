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
                val paths =
                    try {
                        fileSystem.listFiles(CONFIG_DIR, ".txt")
                    } catch (e: IOException) {
                        Timber.w("listFiles failed for config dir")
                        return@withContext emptyList()
                    }

                val results =
                    paths.map { path ->
                        val fileName = path.substringAfterLast('/').ifEmpty { path }
                        try {
                            val bytes = fileSystem.readBytes(path)
                            when (val pr = parser.parse(fileName, bytes)) {
                                is ParseResult.Success -> ConfigLoadResult.Valid(pr.config)
                                is ParseResult.Failure -> ConfigLoadResult.Invalid(fileName, pr.errors)
                            }
                        } catch (e: IOException) {
                            Timber.w("readBytes failed for a config file; marking invalid")
                            ConfigLoadResult.Invalid(
                                fileName = fileName,
                                errors =
                                    listOf(
                                        ParseError(
                                            line = 0,
                                            field = null,
                                            code = ParseErrorCode.CONFIG_FIELD_INVALID,
                                            message = "$fileName: 读取文件失败（IO 异常）",
                                        ),
                                    ),
                            )
                        }
                    }

                results.sortedWith(
                    compareBy { result ->
                        when (result) {
                            is ConfigLoadResult.Valid -> result.config.configId
                            is ConfigLoadResult.Invalid -> result.fileName
                        }
                    },
                )
            }

        companion object {
            private const val CONFIG_DIR = "/sdcard/WenJuanPro/config/"
        }
    }
