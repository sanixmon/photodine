package dev.photodine.feature.canvas.stroke

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatmullRomSplineTest {

    @Test
    fun `interpolation at t 0 equals p1 and at t 1 equals p2`() {
        val p0 = StrokePoint(0f, 0f)
        val p1 = StrokePoint(10f, 10f)
        val p2 = StrokePoint(20f, 30f)
        val p3 = StrokePoint(30f, 40f)

        val atZero = CatmullRomSpline.interpolate(p0, p1, p2, p3, 0f)
        assertEquals(p1.x, atZero.x, 0.001f)
        assertEquals(p1.y, atZero.y, 0.001f)

        val atOne = CatmullRomSpline.interpolate(p0, p1, p2, p3, 1f)
        assertEquals(p2.x, atOne.x, 0.001f)
        assertEquals(p2.y, atOne.y, 0.001f)
    }

    @Test
    fun `single point produces single stamp with pressure scaling`() {
        val pt = StrokePoint(50f, 50f, pressure = 0.8f)
        val stamps = CatmullRomSpline.generateStamps(
            points = listOf(pt),
            baseSize = 20f,
            opacity = 1f,
            color = 0xFF000000.toInt(),
            isEraser = false
        )

        assertEquals(1, stamps.size)
        assertEquals(50f, stamps[0].x)
        assertEquals(50f, stamps[0].y)
        // 20 * (0.5 + 0.8 * 0.5) = 20 * 0.9 = 18f
        assertEquals(18f, stamps[0].size, 0.001f)
    }

    @Test
    fun `stamps are placed with spacing 0 point 2 times brushSize`() {
        val p1 = StrokePoint(0f, 0f, 1f)
        val p2 = StrokePoint(100f, 0f, 1f)
        val stamps = CatmullRomSpline.generateStamps(
            points = listOf(p1, p2),
            baseSize = 20f, // step = 4px -> ~25 stamps along 100px line
            opacity = 1f,
            color = 0xFF000000.toInt(),
            isEraser = false
        )

        assertTrue(stamps.size >= 20, "Expected >= 20 stamps, got ${stamps.size}")
        assertEquals(0f, stamps.first().x, 0.001f)
        assertEquals(100f, stamps.last().x, 0.001f)
    }

    @Test
    fun `eraser flag is preserved on generated stamps`() {
        val pt = StrokePoint(10f, 10f, 1f)
        val stamps = CatmullRomSpline.generateStamps(
            points = listOf(pt),
            baseSize = 30f,
            opacity = 1f,
            color = 0,
            isEraser = true
        )
        assertTrue(stamps.first().isEraser)
    }
}
