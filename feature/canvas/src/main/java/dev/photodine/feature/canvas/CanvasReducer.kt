package dev.photodine.feature.canvas

import dev.photodine.core.engine.CanvasSize

object CanvasReducer {
    fun reduce(
        state: CanvasViewModel.UiState,
        intent: CanvasViewModel.CanvasIntent,
        isEngineReady: Boolean = false
    ): CanvasViewModel.UiState {
        return when (intent) {
            is CanvasViewModel.CanvasIntent.InitCanvas -> {
                if (!CanvasSize.isValid(intent.width, intent.height)) state
                else state.copy(canvasSize = CanvasSize(intent.width, intent.height))
            }
            is CanvasViewModel.CanvasIntent.TransformChanged -> {
                state.copy(transform = intent.transform)
            }
            CanvasViewModel.CanvasIntent.SurfaceActive -> {
                state.copy(engineReady = isEngineReady)
            }
        }
    }
}
