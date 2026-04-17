package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.domain.model.PresentMode
import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreSingleChoiceUseCase
    @Inject
    constructor() {
        operator fun invoke(
            question: Question.SingleChoice,
            answer: Int?,
            stemMs: Long?,
            optionsMs: Long,
        ): ResultRecord {
            val correctText = question.correctIndex.toString()
            if (answer == null) {
                return ResultRecord(
                    qid = question.qid,
                    type = QuestionType.SINGLE,
                    mode = question.mode,
                    stemMs = stemMs.takeIf { question.mode == PresentMode.STAGED },
                    optionsMs = optionsMs,
                    answer = "",
                    correct = correctText,
                    score = 0,
                    status = ResultStatus.NOT_ANSWERED,
                )
            }
            check(answer in 1..question.options.size) {
                "answer index $answer out of range 1..${question.options.size}"
            }
            check(question.scores.size == question.options.size) {
                "scores size ${question.scores.size} does not match options size ${question.options.size}"
            }
            val score = question.scores[answer - 1]
            return ResultRecord(
                qid = question.qid,
                type = QuestionType.SINGLE,
                mode = question.mode,
                stemMs = stemMs.takeIf { question.mode == PresentMode.STAGED },
                optionsMs = optionsMs,
                answer = answer.toString(),
                correct = correctText,
                score = score,
                status = ResultStatus.DONE,
            )
        }
    }
