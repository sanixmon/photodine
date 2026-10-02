package dev.photodine.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CropRectTest {

    @Test
    fun `valid crop rect exposes correct width and height`() {
        val rect = CropRect(left = 10, top = 20, right = 110, bottom = 220)
        assertEquals(100, rect.width)
        assertEquals(200, rect.height)
    }

    @Test
    fun `crop rect from canvas size covers full canvas`() {
        val rect = CropRect.fromCanvasSize(1080, 1920)
        assertEquals(0, rect.left)
        assertEquals(0, rect.top)
        assertEquals(1080, rect.right)
        assertEquals(1920, rect.bottom)
        assertEquals(1080, rect.width)
        assertEquals(1920, rect.height)
    }

    @Test
    fun `dimension below minimum 10px is rejected`() {
        assertThrows<IllegalArgumentException> {
            CropRect(0, 0, 9, 100)
        }
        assertThrows<IllegalArgumentException> {
            CropRect(0, 0, 100, 9)
        }
    }

    @Test
    fun `negative coordinates are rejected`() {
        assertThrows<IllegalArgumentException> {
            CropRect(-1, 0, 100, 100)
        }
        assertThrows<IllegalArgumentException> {
            CropRect(0, -5, 100, 100)
        }
    }
}
