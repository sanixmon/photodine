package dev.photodine.feature.canvas

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dev.photodine.core.engine.CanvasSize
import dev.photodine.core.engine.Compositor
import javax.inject.Inject

/**
 * MVI state for the canvas screen. The [Compositor] is exposed so the
 * `AndroidView` factory can bind the [CanvasTextureView] to the engine
 * without leaking GL types into Compose code.
 */
@HiltViewModel
class CanvasViewModel @Inject constructor(
    val compositor: Compositor
) : ViewModel() {
    data class UiState(
        val canvasSize: CanvasSize? = null,
        val transform: ViewTransform = ViewTransform(),
        val engineReady: Boolean = false
    )

    sealed interface CanvasIntent {
        data class InitCanvas(val width: Int, val height: Int) : CanvasIntent
        data class TransformChanged(val transform: ViewTransform) : CanvasIntent
        data object SurfaceActive : CanvasIntent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIntent(intent: CanvasIntent) {
        val newState = CanvasReducer.reduce(_state.value, intent, compositor.isReady())
        _state.value = newState

        when (intent) {
            is CanvasIntent.InitCanvas -> {
                if (CanvasSize.isValid(intent.width, intent.height)) {
                    compositor.initialiseCanvas(intent.width, intent.height)
                }
            }
            is CanvasIntent.TransformChanged,
            CanvasIntent.SurfaceActive -> {
                // State reduced
            }
        }
    }
}
