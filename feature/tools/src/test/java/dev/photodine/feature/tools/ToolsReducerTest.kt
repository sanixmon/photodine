package dev.photodine.feature.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToolsReducerTest {

    private val initialState = ToolsState()

    @Test
    fun `default tool is brush with 20px black`() {
        assertEquals(ActiveTool.BRUSH, initialState.activeTool)
        assertEquals(20f, initialState.brushSize)
        assertEquals(1f, initialState.brushOpacity)
        assertEquals(0xFF000000.toInt(), initialState.brushColor)
        assertEquals(30f, initialState.eraserSize)
    }

    @Test
    fun `select tool updates active tool`() {
        val next = ToolsReducer.reduce(initialState, ToolsIntent.SelectTool(ActiveTool.MOVE))
        assertEquals(ActiveTool.MOVE, next.activeTool)

        val crop = ToolsReducer.reduce(next, ToolsIntent.SelectTool(ActiveTool.CROP))
        assertEquals(ActiveTool.CROP, crop.activeTool)
    }

    @Test
    fun `brush size clamps between 1 and 500`() {
        val normal = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushSize(120f))
        assertEquals(120f, normal.brushSize)

        val low = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushSize(0.2f))
        assertEquals(1f, low.brushSize)

        val high = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushSize(999f))
        assertEquals(500f, high.brushSize)
    }

    @Test
    fun `brush opacity clamps between 0 and 1`() {
        val normal = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushOpacity(0.75f))
        assertEquals(0.75f, normal.brushOpacity)

        val low = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushOpacity(-0.1f))
        assertEquals(0f, low.brushOpacity)

        val high = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushOpacity(2f))
        assertEquals(1f, high.brushOpacity)
    }

    @Test
    fun `eraser size clamps between 1 and 500`() {
        val normal = ToolsReducer.reduce(initialState, ToolsIntent.SetEraserSize(50f))
        assertEquals(50f, normal.eraserSize)

        val low = ToolsReducer.reduce(initialState, ToolsIntent.SetEraserSize(-10f))
        assertEquals(1f, low.eraserSize)
    }

    @Test
    fun `brush color updates accurately`() {
        val red = 0xFFFF0000.toInt()
        val next = ToolsReducer.reduce(initialState, ToolsIntent.SetBrushColor(red))
        assertEquals(red, next.brushColor)
    }

    @Test
    fun `toggle options toggles visibility`() {
        assertFalse(initialState.isOptionsVisible)
        val expanded = ToolsReducer.reduce(initialState, ToolsIntent.ToggleOptions)
        assertTrue(expanded.isOptionsVisible)
    }
}
