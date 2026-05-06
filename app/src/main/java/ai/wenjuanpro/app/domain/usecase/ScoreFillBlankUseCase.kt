package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreFillBlankUseCase
    @Inject
    constructor() {
        operator fun invoke(
            question: Question.FillBlank,
            answer: String,
            stemMs: Long?,
            optionsMs: Long,
        ): ResultRecord {
            // Inner separator must avoid '|' (CSV record delimiter).
            val correctText = question.acceptableAnswers.joinToString(";")
            val trimmed = answer.trim()
            if (trimmed.isEmpty()) {
                return ResultRecord(
                    qid = question.qid,
                    type = QuestionType.FILL,
                    mode = question.mode,
                    stemMs = stemMs.takeIf { question.mode == PresentMode.STAGED },
                    optionsMs = optionsMs,
                    answer = "",
                    correct = correctText,
                    score = 0,
                    status = ResultStatus.NOT_ANSWERED,
                )
            }
            val matched = matches(trimmed, question.acceptableAnswers, question.caseSensitive)
            // Pipes and newlines would break the pipe-delimited result CSV format,
            // so sanitise them before persisting; matching ran on the raw text.
            val safe = trimmed.replace('|', '/').replace('\n', ' ').replace('\r', ' ')
            return ResultRecord(
                qid = question.qid,
                type = QuestionType.FILL,
                mode = question.mode,
                stemMs = stemMs.takeIf { question.mode == PresentMode.STAGED },
                optionsMs = optionsMs,
                answer = safe,
                correct = correctText,
                score = if (matched) question.score else 0,
                status = ResultStatus.DONE,
            )
        }

        private fun matches(
            user: String,
            acceptable: List<String>,
            caseSensitive: Boolean,
        ): Boolean {
            val target = if (caseSensitive) user else user.lowercase()
            return acceptable.any { candidate ->
                val c = candidate.trim()
                if (caseSensitive) target == c else target == c.lowercase()
            }
        }
    }
