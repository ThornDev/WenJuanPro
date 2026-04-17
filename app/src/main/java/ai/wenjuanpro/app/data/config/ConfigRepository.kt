package ai.wenjuanpro.app.data.config

interface ConfigRepository {
    suspend fun loadAll(): List<ConfigLoadResult>

    suspend fun ensureConfigDir(): Boolean
}
