package dev.photodine.feature.colorpicker

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ColorPickerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ColorPickerState())
    val state: StateFlow<ColorPickerState> = _state.asStateFlow()

    fun onIntent(intent: ColorPickerIntent) {
        _state.update { ColorPickerReducer.reduce(it, intent) }
    }
}
