package ai.wenjuanpro.app.core.io

interface FileSystem {
    fun exists(path: String): Boolean

    fun listFiles(dir: String, suffix: String = ".txt"): List<String>

    fun readBytes(path: String): ByteArray

    fun mkdirs(path: String): Boolean
}
