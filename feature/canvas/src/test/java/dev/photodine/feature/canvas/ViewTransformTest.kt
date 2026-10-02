package dev.photodine.feature.canvas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ViewTransformTest {

    @Test
    fun `pan accumulates offsets`() {
        val next = ViewTransform().withPan(10f, -5f).withPan(3f, 7f)
        assertEquals(13f, next.offsetX)
        assertEquals(2f, next.offsetY)
        assertEquals(1f, next.zoom)
    }

    @Test
    fun `zoom keeps focus point stationary`() {
        val start = ViewTransform(zoom = 1f, offsetX = 100f, offsetY = 50f)
        val zoomed = start.withZoom(2f, focusX = 200f, focusY = 150f)
        assertEquals(2f, zoomed.zoom)
        // focus - (focus - offset) * scale == focus invariant rearranged:
        assertEquals(200f - (200f - 100f) * 2f, zoomed.offsetX)
        assertEquals(150f - (150f - 50f) * 2f, zoomed.offsetY)
    }

    @Test
    fun `zoom clamps to 0 point 1x minimum`() {
        val zoomed = ViewTransform(zoom = 1f).withZoom(0.0001f, 0f, 0f)
        assertEquals(0.1f, zoomed.zoom)
    }

    @Test
    fun `zoom clamps to 32x maximum`() {
        val zoomed = ViewTransform(zoom = 1f).withZoom(1000f, 0f, 0f)
        assertEquals(32f, zoomed.zoom)
    }
}
