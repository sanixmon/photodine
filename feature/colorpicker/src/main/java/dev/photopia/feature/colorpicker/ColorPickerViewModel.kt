package dev.photopia.feature.colorpicker

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ColorPickerViewModel @Inject constructor() : ViewModel() {
    data class UiState(val ready: Boolean = false)

    sealed interface ColorPickerIntent {
        data object Refresh : ColorPickerIntent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIntent(intent: ColorPickerIntent) {
        when (intent) {
            ColorPickerIntent.Refresh -> _state.value = UiState(ready = true)
        }
    }
}
