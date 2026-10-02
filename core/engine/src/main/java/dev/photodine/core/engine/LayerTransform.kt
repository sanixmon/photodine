package dev.photodine.core.engine

import kotlin.math.cos
import kotlin.math.sin

/**
 * Non-destructive 2D affine transform applied to a layer quad at composite time.
 * [translateX] and [translateY] are in canvas space.
 * [scale] is uniform scale factor (clamped 0.05x–20x).
 * [rotationDeg] is counter-clockwise rotation in degrees.
 */
data class LayerTransform(
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f
) {
    /**
     * Returns a 3x3 column-major affine transformation matrix suitable for
     * `glUniformMatrix3fv`.
     */
    fun toMatrix(): FloatArray {
        val rad = Math.toRadians(rotationDeg.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val s = scale.coerceIn(0.05f, 20f)
        return floatArrayOf(
            s * cosA, s * sinA, 0f,
            -s * sinA, s * cosA, 0f,
            translateX, translateY, 1f
        )
    }
}
