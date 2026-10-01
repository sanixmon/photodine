package dev.photopia.feature.canvas

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor() : ViewModel() {
    data class UiState(val ready: Boolean = false)

    sealed interface CanvasIntent {
        data object Refresh : CanvasIntent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIntent(intent: CanvasIntent) {
        when (intent) {
            CanvasIntent.Refresh -> _state.value = UiState(ready = true)
        }
    }
}
