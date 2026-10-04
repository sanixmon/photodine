package dev.photodine.core.engine

/**
 * A single circular stamp quad rendered into a layer texture FBO.
 *
 * @param x Center x coordinate in canvas pixels.
 * @param y Center y coordinate in canvas pixels.
 * @param size Diameter of stamp in pixels.
 * @param opacity Alpha multiplier in 0f..1f.
 * @param color 32-bit ARGB packed color integer.
 * @param isEraser When true, blend mode clears alpha (GL_ZERO, GL_ONE_MINUS_SRC_ALPHA).
 */
data class BrushStamp(
    val x: Float,
    val y: Float,
    val size: Float,
    val opacity: Float = 1f,
    val color: Int = 0xFF000000.toInt(),
    val isEraser: Boolean = false
)
