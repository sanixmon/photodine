package dev.photodine.app.pixellab

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.UUID

enum class EditorTab { Blend, Text, Shapes, Images, Effects }

data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "New Text",
    val color: Color = Color.White,
    val fontSize: TextUnit = 64.sp,
    val offset: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

data class EditorState(
    val layers: List<TextLayer> = listOf(TextLayer()),
    val selectedId: String? = null,
    val activeTab: EditorTab = EditorTab.Text,
    val showGrid: Boolean = false,
    val isEditingText: Boolean = false
) {
    val selectedLayer: TextLayer? get() = layers.firstOrNull { it.id == selectedId }
}
