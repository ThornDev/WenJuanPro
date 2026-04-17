package ai.wenjuanpro.app.feature.configlist

import ai.wenjuanpro.app.data.parser.ParseError
import androidx.annotation.StringRes

sealed interface ConfigListUiState {
    data object Loading : ConfigListUiState

    data class Success(
        val cards: List<ConfigCardUiModel>,
        val allInvalid: Boolean,
    ) : ConfigListUiState

    data object Empty : ConfigListUiState

    data object Timeout : ConfigListUiState

    data class Error(@StringRes val messageRes: Int) : ConfigListUiState
}

data class ConfigCardUiModel(
    val configId: String,
    val title: String,
    val questionCount: Int?,
    val isValid: Boolean,
    val errors: List<ParseError>,
    val sourceFileName: String,
)

data class ErrorSheetState(
    val sourceFileName: String,
    val errors: List<ParseError>,
)
