package dev.photodine.feature.canvas

import dev.photodine.core.engine.CanvasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CanvasReducerTest {

    private val initialState = CanvasViewModel.UiState()

    @Test
    fun `init canvas updates canvas size when valid`() {
        val next = CanvasReducer.reduce(
            initialState,
            CanvasViewModel.CanvasIntent.InitCanvas(1080, 1920)
        )
        assertEquals(CanvasSize(1080, 1920), next.canvasSize)
    }

    @Test
    fun `init canvas ignores out-of-bounds size`() {
        val next = CanvasReducer.reduce(
            initialState,
            CanvasViewModel.CanvasIntent.InitCanvas(5000, 5000)
        )
        assertNull(next.canvasSize)
    }

    @Test
    fun `transform changed updates view transform`() {
        val transform = ViewTransform(zoom = 2f, offsetX = 10f, offsetY = 20f)
        val next = CanvasReducer.reduce(
            initialState,
            CanvasViewModel.CanvasIntent.TransformChanged(transform)
        )
        assertEquals(transform, next.transform)
    }

    @Test
    fun `surface active sets engine ready flag`() {
        val next = CanvasReducer.reduce(
            initialState,
            CanvasViewModel.CanvasIntent.SurfaceActive,
            isEngineReady = true
        )
        assertTrue(next.engineReady)
    }
}
