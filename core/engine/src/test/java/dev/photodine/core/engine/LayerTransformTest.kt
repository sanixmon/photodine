package dev.photodine.core.engine

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LayerTransformTest {

    @Test
    fun `default transform produces identity matrix`() {
        val transform = LayerTransform()
        val expected = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        assertArrayEquals(expected, transform.toMatrix(), 0.0001f)
    }

    @Test
    fun `translation is placed in third column`() {
        val transform = LayerTransform(translateX = 100f, translateY = -50f)
        val matrix = transform.toMatrix()
        assertEquals(100f, matrix[6])
        assertEquals(-50f, matrix[7])
        assertEquals(1f, matrix[8])
    }

    @Test
    fun `scale scales diagonal elements`() {
        val transform = LayerTransform(scale = 2.5f)
        val matrix = transform.toMatrix()
        assertEquals(2.5f, matrix[0], 0.0001f)
        assertEquals(2.5f, matrix[4], 0.0001f)
    }

    @Test
    fun `rotation 90 degrees rotates properly`() {
        val transform = LayerTransform(rotationDeg = 90f)
        val matrix = transform.toMatrix()
        // cos(90) = 0, sin(90) = 1
        assertEquals(0f, matrix[0], 0.001f)
        assertEquals(1f, matrix[1], 0.001f)
        assertEquals(-1f, matrix[3], 0.001f)
        assertEquals(0f, matrix[4], 0.001f)
    }

    @Test
    fun `scale clamps between 0 point 05 and 20`() {
        val minScale = LayerTransform(scale = 0.001f).toMatrix()
        assertEquals(0.05f, minScale[0], 0.0001f)

        val maxScale = LayerTransform(scale = 100f).toMatrix()
        assertEquals(20f, maxScale[0], 0.0001f)
    }
}
