package dev.photodine.feature.layers

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.photodine.core.engine.Compositor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LayersViewModel @Inject constructor(
    private val compositor: Compositor
) : ViewModel() {

    private val _state = MutableStateFlow(
        LayerState(
            layers = compositor.layers,
            activeLayerId = compositor.layers.firstOrNull()?.id
        )
    )
    val state: StateFlow<LayerState> = _state.asStateFlow()

    fun onIntent(intent: LayerIntent) {
        val oldState = _state.value
        val newState = LayerReducer.reduce(oldState, intent)
        _state.value = newState

        // Synchronize engine layer state
        when (intent) {
            is LayerIntent.AddLayer -> {
                val added = newState.activeLayer
                if (added != null) {
                    val idx = newState.layers.indexOfFirst { it.id == added.id }
                    compositor.addLayer(added, idx)
                }
            }

            is LayerIntent.DeleteLayer -> {
                compositor.removeLayer(intent.id)
            }

            is LayerIntent.DuplicateLayer -> {
                val dup = newState.activeLayer
                if (dup != null) {
                    val idx = newState.layers.indexOfFirst { it.id == dup.id }
                    compositor.addLayer(dup, idx)
                }
            }

            is LayerIntent.ReorderLayer -> {
                compositor.reorderLayers(intent.fromIndex, intent.toIndex)
            }

            is LayerIntent.SetVisibility,
            is LayerIntent.SetOpacity,
            is LayerIntent.SetBlendMode -> {
                val targetId = when (intent) {
                    is LayerIntent.SetVisibility -> intent.id
                    is LayerIntent.SetOpacity -> intent.id
                    is LayerIntent.SetBlendMode -> intent.id
                    else -> null
                }
                if (targetId != null) {
                    val updated = newState.layers.firstOrNull { it.id == targetId }
                    if (updated != null) {
                        compositor.updateLayer(updated)
                    }
                }
            }

            is LayerIntent.SelectLayer,
            LayerIntent.ToggleExpanded -> {
                // UI-only state change
            }
        }
    }
}
