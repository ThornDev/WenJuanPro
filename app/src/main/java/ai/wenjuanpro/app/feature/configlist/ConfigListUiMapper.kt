package ai.wenjuanpro.app.feature.configlist

import ai.wenjuanpro.app.data.config.ConfigLoadResult

object ConfigListUiMapper {
    fun toCards(results: List<ConfigLoadResult>): List<ConfigCardUiModel> =
        results.map { result ->
            when (result) {
                is ConfigLoadResult.Valid ->
                    ConfigCardUiModel(
                        configId = result.config.configId,
                        title = result.config.title,
                        questionCount = result.config.questions.size,
                        isValid = true,
                        errors = emptyList(),
                        sourceFileName = result.config.sourceFileName,
                    )
                is ConfigLoadResult.Invalid ->
                    ConfigCardUiModel(
                        configId = result.fileName.removeSuffix(".txt"),
                        title = result.fileName,
                        questionCount = null,
                        isValid = false,
                        errors = result.errors,
                        sourceFileName = result.fileName,
                    )
            }
        }
}
