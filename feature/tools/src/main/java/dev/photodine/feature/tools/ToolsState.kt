package dev.photodine.feature.tools

enum class ActiveTool {
    BRUSH,
    ERASER,
    MOVE,
    CROP
}

enum class PixelLabTab(val label: String, val icon: String) {
    PRESETS("Presets", "◫"),
    DRAW("Draw", "🖌"),
    OBJECT("Object", "⬡"),
    CANVAS("Canvas", "❐"),
    COLOR("Color", "🎨")
}

data class ToolsState(
    val activeTool: ActiveTool = ActiveTool.BRUSH,
    val selectedTab: PixelLabTab = PixelLabTab.DRAW,
    val brushSize: Float = 20f,
    val brushOpacity: Float = 1f,
    val brushColor: Int = 0xFF000000.toInt(),
    val eraserSize: Float = 30f,
    val isOptionsVisible: Boolean = false
)

sealed interface ToolsIntent {
    data class SelectTool(val tool: ActiveTool) : ToolsIntent
    data class SelectTab(val tab: PixelLabTab) : ToolsIntent
    data class SetBrushSize(val size: Float) : ToolsIntent
    data class SetBrushOpacity(val opacity: Float) : ToolsIntent
    data class SetBrushColor(val color: Int) : ToolsIntent
    data class SetEraserSize(val size: Float) : ToolsIntent
    data object ToggleOptions : ToolsIntent
}

object ToolsReducer {
    fun reduce(state: ToolsState, intent: ToolsIntent): ToolsState {
        return when (intent) {
            is ToolsIntent.SelectTool -> state.copy(activeTool = intent.tool)
            is ToolsIntent.SelectTab -> state.copy(selectedTab = intent.tab)
            is ToolsIntent.SetBrushSize -> state.copy(brushSize = intent.size.coerceIn(1f, 500f))
            is ToolsIntent.SetBrushOpacity -> state.copy(brushOpacity = intent.opacity.coerceIn(0f, 1f))
            is ToolsIntent.SetBrushColor -> state.copy(brushColor = intent.color)
            is ToolsIntent.SetEraserSize -> state.copy(eraserSize = intent.size.coerceIn(1f, 500f))
            ToolsIntent.ToggleOptions -> state.copy(isOptionsVisible = !state.isOptionsVisible)
        }
    }
}
