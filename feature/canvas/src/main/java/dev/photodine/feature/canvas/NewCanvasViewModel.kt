package dev.photodine.feature.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.photodine.core.engine.CanvasSize
import javax.inject.Inject

/**
 * MVI state for the new-canvas flow. Preset taps navigate immediately;
 * custom WxH input is validated against [CanvasSize] limits (max 2048px).
 */
@HiltViewModel
class NewCanvasViewModel @Inject constructor() : ViewModel() {
    data class UiState(
        val customWidth: String = "1080",
        val customHeight: String = "1080",
        val error: String? = null
    )

    sealed interface NewCanvasIntent {
        data class PresetSelected(val size: CanvasSize) : NewCanvasIntent
        data class WidthChanged(val value: String) : NewCanvasIntent
        data class HeightChanged(val value: String) : NewCanvasIntent
        data object ConfirmCustom : NewCanvasIntent
        data object DismissError : NewCanvasIntent
    }

    sealed interface NewCanvasEffect {
        data class NavigateToCanvas(val width: Int, val height: Int) : NewCanvasEffect
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<NewCanvasEffect>()
    val effects: SharedFlow<NewCanvasEffect> = _effects.asSharedFlow()

    fun onIntent(intent: NewCanvasIntent) {
        when (intent) {
            is NewCanvasIntent.PresetSelected -> navigate(intent.size.width, intent.size.height)
            is NewCanvasIntent.WidthChanged ->
                _state.update { it.copy(customWidth = intent.value, error = null) }

            is NewCanvasIntent.HeightChanged ->
                _state.update { it.copy(customHeight = intent.value, error = null) }

            NewCanvasIntent.ConfirmCustom -> {
                val width = _state.value.customWidth.toIntOrNull()
                val height = _state.value.customHeight.toIntOrNull()
                if (width == null || height == null || !CanvasSize.isValid(width, height)) {
                    _state.update {
                        it.copy(error = "Enter width and height from 1 to ${CanvasSize.MAX_DIMENSION}px")
                    }
                } else {
                    navigate(width, height)
                }
            }

            NewCanvasIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun navigate(width: Int, height: Int) {
        viewModelScope.launch {
            _effects.emit(NewCanvasEffect.NavigateToCanvas(width, height))
        }
    }
}
