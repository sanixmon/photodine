package dev.photodine.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CanvasSizeTest {

    @Test
    fun `presets are within limits`() {
        CanvasSize.PRESETS.forEach {
            assertTrue(CanvasSize.isValid(it.width, it.height), "preset $it invalid")
        }
    }

    @Test
    fun `expected presets exist`() {
        assertEquals(
            listOf(CanvasSize(1080, 1080), CanvasSize(1920, 1080), CanvasSize(2048, 2048)),
            CanvasSize.PRESETS
        )
    }

    @Test
    fun `max dimension boundary is valid`() {
        assertTrue(CanvasSize.isValid(2048, 2048))
        assertTrue(CanvasSize.isValid(1, 1))
    }

    @Test
    fun `over-limit dimensions are invalid`() {
        assertFalse(CanvasSize.isValid(2049, 1080))
        assertFalse(CanvasSize.isValid(1080, 2049))
        assertFalse(CanvasSize.isValid(0, 1080))
        assertFalse(CanvasSize.isValid(-1, 1080))
    }

    @Test
    fun `constructor rejects out-of-range dimensions`() {
        assertThrows<IllegalArgumentException> { CanvasSize(0, 1080) }
        assertThrows<IllegalArgumentException> { CanvasSize(1080, 4096) }
    }
}
