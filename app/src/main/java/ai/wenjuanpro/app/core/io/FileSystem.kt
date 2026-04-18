package ai.wenjuanpro.app.core.io

import java.io.FileOutputStream

interface FileSystem {
    fun exists(path: String): Boolean

    fun listFiles(dir: String, suffix: String = ".txt"): List<String>

    fun readBytes(path: String): ByteArray

    fun mkdirs(path: String): Boolean

    /** Opens (or creates) a file in append mode and returns its raw [FileOutputStream]. */
    fun openAppend(path: String): FileOutputStream

    /** Creates the file at [path] if it does not already exist. */
    fun createFileIfNotExists(path: String)

    /** Deletes the file at [path] if it exists. */
    fun deleteIfExists(path: String)
}
