package ai.wenjuanpro.app.feature.scan

sealed interface ScanEffect {
    data class NavigateToWelcome(val studentId: String, val configId: String) : ScanEffect

    data object NavigateBack : ScanEffect

    data object OpenAppSettings : ScanEffect

    data object RequestCameraPermission : ScanEffect
}
