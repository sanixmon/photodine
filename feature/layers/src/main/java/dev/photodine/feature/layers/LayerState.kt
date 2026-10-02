package dev.photodine.feature.layers

import dev.photodine.core.engine.BlendMode
import dev.photodine.core.engine.Layer
import java.util.UUID

sealed interface LayerIntent {
    data class AddLayer(val name: String? = null) : LayerIntent
    data class DeleteLayer(val id: UUID) : LayerIntent
    data class DuplicateLayer(val id: UUID) : LayerIntent
    data class ReorderLayer(val fromIndex: Int, val toIndex: Int) : LayerIntent
    data class SetVisibility(val id: UUID, val visible: Boolean) : LayerIntent
    data class SetOpacity(val id: UUID, val opacity: Float) : LayerIntent
    data class SetBlendMode(val id: UUID, val blendMode: BlendMode) : LayerIntent
    data class SelectLayer(val id: UUID) : LayerIntent
    data object ToggleExpanded : LayerIntent
}

data class LayerState(
    val layers: List<Layer> = emptyList(),
    val activeLayerId: UUID? = null,
    val isExpanded: Boolean = false
) {
    val activeLayer: Layer? get() = layers.firstOrNull { it.id == activeLayerId }
}

object LayerReducer {

    fun reduce(
        state: LayerState,
        intent: LayerIntent,
        nextTextureId: () -> Int = { 0 }
    ): LayerState {
        return when (intent) {
            is LayerIntent.SelectLayer -> {
                if (state.layers.any { it.id == intent.id }) {
                    state.copy(activeLayerId = intent.id)
                } else {
                    state
                }
            }

            is LayerIntent.AddLayer -> {
                val activeIdx = state.layers.indexOfFirst { it.id == state.activeLayerId }
                val insertIdx = if (activeIdx >= 0) activeIdx + 1 else state.layers.size
                val newLayer = Layer(
                    id = UUID.randomUUID(),
                    name = intent.name ?: "Layer ${state.layers.size + 1}",
                    textureId = nextTextureId()
                )
                val newLayers = state.layers.toMutableList().apply {
                    add(insertIdx, newLayer)
                }
                state.copy(layers = newLayers, activeLayerId = newLayer.id)
            }

            is LayerIntent.DeleteLayer -> {
                // Must keep at least one layer
                if (state.layers.size <= 1) return state
                val idx = state.layers.indexOfFirst { it.id == intent.id }
                if (idx == -1) return state

                val newLayers = state.layers.toMutableList().apply { removeAt(idx) }
                val newActiveId = if (state.activeLayerId == intent.id) {
                    val nextIdx = idx.coerceAtMost(newLayers.size - 1)
                    newLayers[nextIdx].id
                } else {
                    state.activeLayerId
                }
                state.copy(layers = newLayers, activeLayerId = newActiveId)
            }

            is LayerIntent.DuplicateLayer -> {
                val idx = state.layers.indexOfFirst { it.id == intent.id }
                if (idx == -1) return state
                val source = state.layers[idx]
                val duplicate = source.copy(
                    id = UUID.randomUUID(),
                    name = "${source.name} copy",
                    textureId = nextTextureId()
                )
                val newLayers = state.layers.toMutableList().apply {
                    add(idx + 1, duplicate)
                }
                state.copy(layers = newLayers, activeLayerId = duplicate.id)
            }

            is LayerIntent.ReorderLayer -> {
                if (intent.fromIndex !in state.layers.indices || intent.toIndex !in state.layers.indices) {
                    return state
                }
                if (intent.fromIndex == intent.toIndex) return state
                val newLayers = state.layers.toMutableList()
                val moved = newLayers.removeAt(intent.fromIndex)
                newLayers.add(intent.toIndex, moved)
                state.copy(layers = newLayers)
            }

            is LayerIntent.SetVisibility -> {
                val newLayers = state.layers.map {
                    if (it.id == intent.id) it.copy(visible = intent.visible) else it
                }
                state.copy(layers = newLayers)
            }

            is LayerIntent.SetOpacity -> {
                val clamped = intent.opacity.coerceIn(0f, 1f)
                val newLayers = state.layers.map {
                    if (it.id == intent.id) it.copy(opacity = clamped) else it
                }
                state.copy(layers = newLayers)
            }

            is LayerIntent.SetBlendMode -> {
                val newLayers = state.layers.map {
                    if (it.id == intent.id) it.copy(blendMode = intent.blendMode) else it
                }
                state.copy(layers = newLayers)
            }

            LayerIntent.ToggleExpanded -> state.copy(isExpanded = !state.isExpanded)
        }
    }
}
