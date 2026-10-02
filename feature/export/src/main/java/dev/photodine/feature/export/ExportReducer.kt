package dev.photodine.feature.export

object ExportReducer {
    fun reduce(state: ExportUiState, intent: ExportIntent): ExportUiState {
        return when (intent) {
            is ExportIntent.SetFormat -> state.copy(format = intent.format)
            is ExportIntent.SetQuality -> state.copy(jpegQuality = intent.quality.coerceIn(10, 100))
            is ExportIntent.Export -> state.copy(isExporting = true, errorMessage = null)
            ExportIntent.DismissMessage -> state.copy(exportSuccessUri = null, errorMessage = null)
        }
    }
}
