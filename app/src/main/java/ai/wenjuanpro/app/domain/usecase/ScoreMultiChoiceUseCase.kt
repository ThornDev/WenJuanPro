package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreMultiChoiceUseCase
    @Inject
    constructor() {
        operator fun invoke(
            question: Question.MultiChoice,
            selectedIndices: Set<Int>?,
            stemMs: Long?,
            optionsMs: Long,
        ): ResultRecord {
            val correctText = question.correctIndices.sorted().joinToString(",")
            val stemMsForMode = stemMs.takeIf { question.mode == PresentMode.STAGED }

            if (selectedIndices.isNullOrEmpty()) {
                return ResultRecord(
                    qid = question.qid,
                    type = QuestionType.MULTI,
                    mode = question.mode,
                    stemMs = stemMsForMode,
                    optionsMs = optionsMs,
                    answer = "",
                    correct = correctText,
                    score = 0,
                    status = ResultStatus.NOT_ANSWERED,
                )
            }

            check(question.scores.size == question.options.size) {
                "scores size ${question.scores.size} does not match options size ${question.options.size}"
            }
            selectedIndices.forEach { idx ->
                check(idx in 1..question.options.size) {
                    "answer index $idx out of range 1..${question.options.size}"
                }
            }

            val sortedIndices = selectedIndices.sorted()
            val totalScore = sortedIndices.sumOf { question.scores[it - 1] }
            val answerCsv = sortedIndices.joinToString(",")

            return ResultRecord(
                qid = question.qid,
                type = QuestionType.MULTI,
                mode = question.mode,
                stemMs = stemMsForMode,
                optionsMs = optionsMs,
                answer = answerCsv,
                correct = correctText,
                score = totalScore,
                status = ResultStatus.DONE,
            )
        }
    }
