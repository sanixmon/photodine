package dev.photopia.feature.tools

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor() : ViewModel() {
    data class UiState(val ready: Boolean = false)

    sealed interface ToolsIntent {
        data object Refresh : ToolsIntent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIntent(intent: ToolsIntent) {
        when (intent) {
            ToolsIntent.Refresh -> _state.value = UiState(ready = true)
        }
    }
}
