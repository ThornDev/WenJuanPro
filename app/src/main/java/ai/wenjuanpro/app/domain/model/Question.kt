package ai.wenjuanpro.app.domain.model

enum class PresentMode { ALL_IN_ONE, STAGED }

sealed interface StemContent {
    data class Text(val text: String) : StemContent
    data class Image(val fileName: String) : StemContent
    data class Mixed(val parts: List<StemContent>) : StemContent
}

sealed interface OptionContent {
    data class Text(val text: String) : OptionContent
    data class Image(val fileName: String) : OptionContent
    data class Mixed(val parts: List<OptionContent>) : OptionContent
}

sealed interface Question {
    val qid: String
    val mode: PresentMode
    val stemDurationMs: Long?
    val optionsDurationMs: Long
    val totalDurationMs: Long
        get() = (stemDurationMs ?: 0L) + optionsDurationMs

    data class SingleChoice(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,
        val stem: StemContent,
        val options: List<OptionContent>,
        val correctIndex: Int,
        val scores: List<Int>,
    ) : Question

    data class MultiChoice(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,
        val stem: StemContent,
        val options: List<OptionContent>,
        val correctIndices: Set<Int>,
        val scores: List<Int>,
    ) : Question

    data class Memory(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,
        val dotsPositions: List<Int>,
        val flashDurationMs: Long = 1000L,
        val flashIntervalMs: Long = 500L,
    ) : Question
}
