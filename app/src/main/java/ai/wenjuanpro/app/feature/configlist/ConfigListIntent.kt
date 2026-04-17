package ai.wenjuanpro.app.feature.configlist

sealed interface ConfigListIntent {
    data object Refresh : ConfigListIntent

    data class CardClicked(val configId: String) : ConfigListIntent

    data class ViewErrors(val sourceFileName: String) : ConfigListIntent

    data object DismissSheet : ConfigListIntent

    data object HiddenAreaLongPressed : ConfigListIntent
}

sealed interface ConfigListEffect {
    data class NavigateToScan(val configId: String) : ConfigListEffect

    data object NavigateToDiagnostics : ConfigListEffect
}
