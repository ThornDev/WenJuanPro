package ai.wenjuanpro.app.domain.model

data class Config(
    val configId: String,
    val title: String,
    val sourceFileName: String,
    val questions: List<Question>,
    val parseWarnings: List<ConfigWarning> = emptyList(),
) {
    val totalDurationMs: Long
        get() = questions.sumOf { it.totalDurationMs }
}

data class ConfigWarning(val line: Int, val message: String)
