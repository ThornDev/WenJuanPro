package ai.wenjuanpro.app.domain.model

enum class QuestionType { SINGLE, MULTI, MEMORY }

enum class ResultStatus { DONE, NOT_ANSWERED, PARTIAL, ERROR }

data class ResultRecord(
    val qid: String,
    val type: QuestionType,
    val mode: PresentMode,
    val stemMs: Long?,
    val optionsMs: Long,
    val answer: String,
    val correct: String,
    val score: Int,
    val status: ResultStatus,
)
