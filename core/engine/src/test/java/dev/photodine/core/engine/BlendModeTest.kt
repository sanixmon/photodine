package dev.photodine.core.engine

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlendModeTest {

    private val red = floatArrayOf(1f, 0f, 0f, 1f)
    private val blue = floatArrayOf(0f, 0f, 1f, 1f)

    private fun blend(mode: BlendMode, opacity: Float = 0.5f): FloatArray {
        return BlendMode.composite(
            dstR = red[0], dstG = red[1], dstB = red[2], dstA = red[3],
            srcR = blue[0], srcG = blue[1], srcB = blue[2], srcA = blue[3],
            opacity = opacity,
            mode = mode
        )
    }

    @Test
    fun `normal blend with 50 percent opacity matches expected color`() {
        val result = blend(BlendMode.NORMAL, 0.5f)
        // mix((1,0,0), (0,0,1), 0.5) = (0.5, 0.0, 0.5, 1.0)
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0.5f, 1f), result, 0.001f)
    }

    @Test
    fun `multiply blend with 50 percent opacity matches expected color`() {
        val result = blend(BlendMode.MULTIPLY, 0.5f)
        // b = (0, 0, 0); mix((1,0,0), (0,0,0), 0.5) = (0.5, 0.0, 0.0, 1.0)
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0f, 1f), result, 0.001f)
    }

    @Test
    fun `screen blend with 50 percent opacity matches expected color`() {
        val result = blend(BlendMode.SCREEN, 0.5f)
        // b = 1 - (1-red)*(1-blue) = (1, 0, 1); mix((1,0,0), (1,0,1), 0.5) = (1.0, 0.0, 0.5, 1.0)
        assertArrayEquals(floatArrayOf(1.0f, 0f, 0.5f, 1f), result, 0.001f)
    }

    @Test
    fun `overlay blend with 50 percent opacity matches expected color`() {
        val result = blend(BlendMode.OVERLAY, 0.5f)
        // red dst >= 0.5 -> 1 - 2*(1-1)*(1-0) = 1.0; green < 0.5 -> 0; blue < 0.5 -> 0
        // b = (1, 0, 0); mix((1,0,0), (1,0,0), 0.5) = (1.0, 0.0, 0.0, 1f)
        assertArrayEquals(floatArrayOf(1.0f, 0f, 0f, 1f), result, 0.001f)
    }

    @Test
    fun `darken blend with 50 percent opacity matches expected color`() {
        val result = blend(BlendMode.DARKEN, 0.5f)
        // b = min((1,0,0), (0,0,1)) = (0,0,0); mix((1,0,0), (0,0,0), 0.5) = (0.5, 0.0, 0.0, 1.0)
        assertArrayEquals(floatArrayOf(0.5f, 0f, 0f, 1f), result, 0.001f)
    }

    @Test
    fun `lighten blend with 50 percent opacity matches expected color`() {
        val result = blend(BlendMode.LIGHTEN, 0.5f)
        // b = max((1,0,0), (0,0,1)) = (1,0,1); mix((1,0,0), (1,0,1), 0.5) = (1.0, 0.0, 0.5, 1.0)
        assertArrayEquals(floatArrayOf(1.0f, 0f, 0.5f, 1f), result, 0.001f)
    }

    @Test
    fun `opacity 0 produces identical dst layer across all modes`() {
        for (mode in BlendMode.entries) {
            val result = blend(mode, 0f)
            assertArrayEquals(red, result, 0.001f, "Mode $mode at opacity 0 failed")
        }
    }

    @Test
    fun `opacity 1 produces fully opaque source blend`() {
        val normal = blend(BlendMode.NORMAL, 1f)
        assertArrayEquals(blue, normal, 0.001f)

        val multiply = blend(BlendMode.MULTIPLY, 1f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), multiply, 0.001f)

        val screen = blend(BlendMode.SCREEN, 1f)
        assertArrayEquals(floatArrayOf(1f, 0f, 1f, 1f), screen, 0.001f)
    }
}
