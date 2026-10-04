package dev.photodine.feature.canvas.stroke

import dev.photodine.core.engine.BrushStamp
import kotlin.math.sqrt

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

object CatmullRomSpline {

    /**
     * Evaluates a Catmull-Rom spline segment given 4 control points at [t] in 0f..1f.
     */
    fun interpolate(
        p0: StrokePoint,
        p1: StrokePoint,
        p2: StrokePoint,
        p3: StrokePoint,
        t: Float
    ): StrokePoint {
        val t2 = t * t
        val t3 = t2 * t

        fun calc(v0: Float, v1: Float, v2: Float, v3: Float): Float {
            return 0.5f * (
                (2f * v1) +
                (-v0 + v2) * t +
                (2f * v0 - 5f * v1 + 4f * v2 - v3) * t2 +
                (-v0 + 3f * v1 - 3f * v2 + v3) * t3
            )
        }

        val x = calc(p0.x, p1.x, p2.x, p3.x)
        val y = calc(p0.y, p1.y, p2.y, p3.y)
        val pressure = calc(p0.pressure, p1.pressure, p2.pressure, p3.pressure).coerceIn(0f, 1f)
        return StrokePoint(x, y, pressure)
    }

    /**
     * Subdivides the spline connecting [points] and produces [BrushStamp]s placed
     * at arc-length intervals of `brushSize * 0.2` px.
     */
    fun generateStamps(
        points: List<StrokePoint>,
        baseSize: Float,
        opacity: Float,
        color: Int,
        isEraser: Boolean
    ): List<BrushStamp> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) {
            val p = points.first()
            val effectiveSize = if (p.pressure > 0f) baseSize * (0.5f + p.pressure * 0.5f) else baseSize
            return listOf(BrushStamp(p.x, p.y, effectiveSize, opacity, color, isEraser))
        }

        val stepDistance = (baseSize * 0.2f).coerceAtLeast(1f)
        val stamps = mutableListOf<BrushStamp>()

        // Pad start and end control points for smooth ends
        val padded = mutableListOf<StrokePoint>().apply {
            add(points.first())
            addAll(points)
            add(points.last())
        }

        for (i in 1 until padded.size - 2) {
            val p0 = padded[i - 1]
            val p1 = padded[i]
            val p2 = padded[i + 1]
            val p3 = padded[i + 2]

            // Approximate segment chord length
            val chord = sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y))
            val steps = (chord / stepDistance).toInt().coerceAtLeast(1)

            for (step in 0..steps) {
                val t = step.toFloat() / steps
                val pt = interpolate(p0, p1, p2, p3, t)
                val effectiveSize = if (pt.pressure > 0f) baseSize * (0.5f + pt.pressure * 0.5f) else baseSize
                stamps.add(
                    BrushStamp(
                        x = pt.x,
                        y = pt.y,
                        size = effectiveSize,
                        opacity = opacity,
                        color = color,
                        isEraser = isEraser
                    )
                )
            }
        }

        return stamps
    }
}
