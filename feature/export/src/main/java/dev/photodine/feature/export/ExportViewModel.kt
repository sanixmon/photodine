package dev.photodine.feature.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.photodine.core.engine.Compositor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExportUiState(
    val format: ExportFormat = ExportFormat.PNG,
    val jpegQuality: Int = 90,
    val isExporting: Boolean = false,
    val exportSuccessUri: Uri? = null,
    val errorMessage: String? = null
)

sealed interface ExportIntent {
    data class SetFormat(val format: ExportFormat) : ExportIntent
    data class SetQuality(val quality: Int) : ExportIntent
    data class Export(val andShare: Boolean = false) : ExportIntent
    data object DismissMessage : ExportIntent
}

sealed interface ExportEffect {
    data class ShareImage(val uri: Uri, val mimeType: String) : ExportEffect
    data class ShowSnackbar(val message: String) : ExportEffect
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val compositor: Compositor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ExportEffect>()
    val effects: SharedFlow<ExportEffect> = _effects.asSharedFlow()

    fun onIntent(intent: ExportIntent) {
        when (intent) {
            is ExportIntent.SetFormat -> _state.update { it.copy(format = intent.format) }
            is ExportIntent.SetQuality -> _state.update { it.copy(jpegQuality = intent.quality.coerceIn(10, 100)) }
            is ExportIntent.Export -> executeExport(intent.andShare)
            ExportIntent.DismissMessage -> _state.update { it.copy(exportSuccessUri = null, errorMessage = null) }
        }
    }

    private fun executeExport(andShare: Boolean) {
        if (_state.value.isExporting) return
        _state.update { it.copy(isExporting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val format = _state.value.format
                val quality = _state.value.jpegQuality
                val w = compositor.canvasWidth
                val h = compositor.canvasHeight

                val uri = withContext(Dispatchers.IO) {
                    val buffer = compositor.flatten(w, h)
                    val bitmap = ExportManager.createBitmapFromGlBuffer(buffer, w, h, format)
                    val saveResult = ExportManager.saveToMediaStore(context, bitmap, format, quality)
                    bitmap.recycle()
                    saveResult.getOrThrow()
                }

                _state.update { it.copy(isExporting = false, exportSuccessUri = uri) }
                _effects.emit(ExportEffect.ShowSnackbar("Saved to Gallery"))

                if (andShare) {
                    _effects.emit(ExportEffect.ShareImage(uri, format.mimeType))
                }
            } catch (e: Exception) {
                val message = e.message ?: "Export failed"
                _state.update { it.copy(isExporting = false, errorMessage = message) }
                _effects.emit(ExportEffect.ShowSnackbar("Export failed: $message"))
            }
        }
    }
}
