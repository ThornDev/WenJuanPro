package ai.wenjuanpro.app.domain.usecase

import ai.wenjuanpro.app.domain.model.Question
import ai.wenjuanpro.app.domain.model.QuestionType
import ai.wenjuanpro.app.domain.model.ResultRecord
import ai.wenjuanpro.app.domain.model.ResultStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreMemoryUseCase
    @Inject
    constructor() {
        operator fun invoke(
            question: Question.Memory,
            answer: List<Int>,
            expected: List<Int>,
            optionsMs: Long,
        ): ResultRecord {
            if (answer.isEmpty()) {
                return ResultRecord(
                    qid = question.qid,
                    type = QuestionType.MEMORY,
                    mode = question.mode,
                    stemMs = null,
                    optionsMs = optionsMs,
                    answer = "",
                    correct = expected.joinToString(","),
                    score = 0,
                    status = ResultStatus.NOT_ANSWERED,
                )
            }

            var prefixMatch = 0
            for (i in answer.indices) {
                if (i < expected.size && answer[i] == expected[i]) {
                    prefixMatch++
                } else {
                    break
                }
            }

            return ResultRecord(
                qid = question.qid,
                type = QuestionType.MEMORY,
                mode = question.mode,
                stemMs = null,
                optionsMs = optionsMs,
                answer = answer.joinToString(","),
                correct = expected.joinToString(","),
                score = prefixMatch,
                status = ResultStatus.DONE,
            )
        }
    }
