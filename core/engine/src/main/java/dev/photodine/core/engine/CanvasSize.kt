package dev.photodine.core.engine

/**
 * Dimensions of a canvas in pixels.
 *
 * Both axes are capped at [MAX_DIMENSION]: layer textures are single
 * RGBA8 `GL_TEXTURE_2D`s and the engine does no tiled rendering in v1.
 */
data class CanvasSize(val width: Int, val height: Int) {
    init {
        require(width in MIN_DIMENSION..MAX_DIMENSION) {
            "Canvas width $width out of range 1..$MAX_DIMENSION"
        }
        require(height in MIN_DIMENSION..MAX_DIMENSION) {
            "Canvas height $height out of range 1..$MAX_DIMENSION"
        }
    }

    companion object {
        const val MIN_DIMENSION = 1
        const val MAX_DIMENSION = 2048

        val PRESETS: List<CanvasSize> = listOf(
            CanvasSize(1080, 1080),
            CanvasSize(1920, 1080),
            CanvasSize(2048, 2048)
        )

        fun isValid(width: Int, height: Int): Boolean =
            width in MIN_DIMENSION..MAX_DIMENSION &&
                height in MIN_DIMENSION..MAX_DIMENSION
    }
}
