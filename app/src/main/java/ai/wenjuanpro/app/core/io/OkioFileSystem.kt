package ai.wenjuanpro.app.core.io

import okio.Path.Companion.toPath
import okio.buffer
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import okio.FileSystem as OkioFs

@Singleton
class OkioFileSystem
    @Inject
    constructor(
        private val delegate: OkioFs = OkioFs.SYSTEM,
    ) : FileSystem {
        override fun exists(path: String): Boolean = delegate.exists(path.toPath())

        override fun listFiles(dir: String, suffix: String): List<String> {
            val dirPath = dir.toPath()
            if (!delegate.exists(dirPath)) return emptyList()
            val meta = delegate.metadataOrNull(dirPath) ?: return emptyList()
            if (!meta.isDirectory) return emptyList()
            return delegate.list(dirPath)
                .filter { child ->
                    val childMeta = delegate.metadataOrNull(child)
                    childMeta?.isRegularFile == true && child.name.endsWith(suffix)
                }
                .map { it.toString() }
                .sorted()
        }

        override fun readBytes(path: String): ByteArray =
            delegate.source(path.toPath()).buffer().use { it.readByteArray() }

        override fun mkdirs(path: String): Boolean {
            val file = File(path)
            if (file.isDirectory) return true
            val created =
                try {
                    file.mkdirs()
                } catch (e: SecurityException) {
                    Timber.w("mkdirs denied by SecurityException")
                    return false
                }
            val ok = created || file.isDirectory
            if (!ok) {
                Timber.w("mkdirs failed path-kind=${describePathKind(file)}")
            }
            return ok
        }

        override fun openAppend(path: String): FileOutputStream =
            FileOutputStream(File(path), true)

        override fun createFileIfNotExists(path: String) {
            val file = File(path)
            if (!file.exists()) {
                file.createNewFile()
            }
        }

        override fun deleteIfExists(path: String) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }

        override fun rename(oldPath: String, newPath: String): Boolean =
            File(oldPath).renameTo(File(newPath))

        private fun describePathKind(file: File): String =
            when {
                file.isFile -> "regular_file"
                file.parentFile?.isFile == true -> "parent_is_file"
                else -> "unknown"
            }
    }
