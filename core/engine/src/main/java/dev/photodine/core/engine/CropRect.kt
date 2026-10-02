package dev.photodine.core.engine

/**
 * Pixel coordinates of a rectangular crop region on the canvas.
 * Coordinates are bounded: [left] < [right] and [top] < [bottom],
 * with minimum width and height of 10px.
 */
data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    init {
        require(left >= 0 && top >= 0) { "Crop offsets must be non-negative" }
        require(width >= MIN_CROP_DIMENSION) {
            "Crop width must be >= $MIN_CROP_DIMENSION, got $width"
        }
        require(height >= MIN_CROP_DIMENSION) {
            "Crop height must be >= $MIN_CROP_DIMENSION, got $height"
        }
    }

    companion object {
        const val MIN_CROP_DIMENSION = 10

        fun fromCanvasSize(width: Int, height: Int): CropRect =
            CropRect(0, 0, width, height)
    }
}
