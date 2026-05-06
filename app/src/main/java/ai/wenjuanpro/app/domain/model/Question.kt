package ai.wenjuanpro.app.domain.model

enum class PresentMode { ALL_IN_ONE, STAGED }

sealed interface StemContent {
    data class Text(val text: String) : StemContent
    data class Image(
        val fileName: String,
        val widthDp: Int? = null,
        val heightDp: Int? = null,
    ) : StemContent
    data class Audio(
        val fileName: String,
        val autoPlay: Boolean = true,
    ) : StemContent
    data class Mixed(val parts: List<StemContent>) : StemContent
}

sealed interface OptionContent {
    data class Text(val text: String) : OptionContent
    data class Image(
        val fileName: String,
        val widthDp: Int? = null,
        val heightDp: Int? = null,
    ) : OptionContent
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
        val showSubmitButton: Boolean = true,
        val autoAdvance: Boolean = false,
        val optionsPerRow: Int? = null,
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
        val showSubmitButton: Boolean = true,
        val optionsPerRow: Int? = null,
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

    data class FillBlank(
        override val qid: String,
        override val mode: PresentMode,
        override val stemDurationMs: Long?,
        override val optionsDurationMs: Long,
        val stem: StemContent,
        val acceptableAnswers: List<String>,
        val score: Int,
        val caseSensitive: Boolean = false,
        val showSubmitButton: Boolean = true,
    ) : Question
}
