package dev.photopia.feature.layers

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LayersViewModel @Inject constructor() : ViewModel() {
    data class UiState(val ready: Boolean = false)

    sealed interface LayersIntent {
        data object Refresh : LayersIntent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIntent(intent: LayersIntent) {
        when (intent) {
            LayersIntent.Refresh -> _state.value = UiState(ready = true)
        }
    }
}
