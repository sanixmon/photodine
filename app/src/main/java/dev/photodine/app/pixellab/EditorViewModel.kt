package dev.photodine.app.pixellab

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class EditorViewModel : ViewModel() {

    private val initialLayer = TextLayer()
    private val _state = MutableStateFlow(
        EditorState(
            layers = listOf(initialLayer),
            selectedId = initialLayer.id
        )
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<List<TextLayer>>()

    private fun pushUndoSnapshot() {
        undoStack.addLast(_state.value.layers)
        if (undoStack.size > 50) {
            undoStack.removeFirst()
        }
    }

    fun selectLayer(id: String?) {
        _state.update { it.copy(selectedId = id) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedId = null) }
    }

    fun selectTab(tab: EditorTab) {
        _state.update { it.copy(activeTab = tab) }
    }

    fun toggleGrid() {
        _state.update { it.copy(showGrid = !it.showGrid) }
    }

    fun addTextLayer() {
        pushUndoSnapshot()
        val newLayer = TextLayer(
            id = UUID.randomUUID().toString(),
            text = "New Text",
            offset = Offset(0f, 0f)
        )
        _state.update {
            it.copy(
                layers = it.layers + newLayer,
                selectedId = newLayer.id
            )
        }
    }

    fun updateText(id: String, newText: String) {
        pushUndoSnapshot()
        _state.update { current ->
            val updated = current.layers.map {
                if (it.id == id) it.copy(text = newText) else it
            }
            current.copy(layers = updated, isEditingText = false)
        }
    }

    fun updateLayerTransform(id: String, offset: Offset, scale: Float, rotation: Float) {
        _state.update { current ->
            val updated = current.layers.map {
                if (it.id == id) {
                    it.copy(
                        offset = offset,
                        scale = scale.coerceIn(0.2f, 5f),
                        rotation = rotation
                    )
                } else {
                    it
                }
            }
            current.copy(layers = updated)
        }
    }

    fun deleteSelected() {
        val selId = _state.value.selectedId ?: return
        pushUndoSnapshot()
        _state.update { current ->
            val updated = current.layers.filter { it.id != selId }
            current.copy(layers = updated, selectedId = null)
        }
    }

    fun copySelected() {
        val selected = _state.value.selectedLayer ?: return
        pushUndoSnapshot()
        val duplicated = selected.copy(
            id = UUID.randomUUID().toString(),
            offset = selected.offset + Offset(32f, 32f)
        )
        _state.update { current ->
            current.copy(
                layers = current.layers + duplicated,
                selectedId = duplicated.id
            )
        }
    }

    fun bringToFront() {
        val selected = _state.value.selectedLayer ?: return
        pushUndoSnapshot()
        _state.update { current ->
            val updated = current.layers.filter { it.id != selected.id } + selected
            current.copy(layers = updated)
        }
    }

    fun sendToBack() {
        val selected = _state.value.selectedLayer ?: return
        pushUndoSnapshot()
        _state.update { current ->
            val updated = listOf(selected) + current.layers.filter { it.id != selected.id }
            current.copy(layers = updated)
        }
    }

    fun showEditDialog(show: Boolean) {
        _state.update { it.copy(isEditingText = show) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        _state.update { current ->
            current.copy(
                layers = previous,
                selectedId = previous.firstOrNull { it.id == current.selectedId }?.id
            )
        }
    }
}
