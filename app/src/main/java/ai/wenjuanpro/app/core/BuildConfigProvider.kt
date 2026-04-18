package ai.wenjuanpro.app.core

/**
 * Provides the application version string for result-file headers.
 */
fun interface BuildConfigProvider {
    fun appVersion(): String
}
