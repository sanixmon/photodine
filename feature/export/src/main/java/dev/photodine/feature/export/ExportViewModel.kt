package dev.photodine.feature.export

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor() : ViewModel() {
    data class UiState(val ready: Boolean = false)

    sealed interface ExportIntent {
        data object Refresh : ExportIntent
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onIntent(intent: ExportIntent) {
        when (intent) {
            ExportIntent.Refresh -> _state.value = UiState(ready = true)
        }
    }
}
