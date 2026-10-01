package dev.photodine.feature.canvas

import dev.photodine.core.engine.GlCompositor

/**
 * Pan/zoom view transform. Offsets are surface-pixel coordinates of the
 * canvas origin (top-left), matching [GlCompositor.setViewTransform].
 *
 * Pure value type: gesture math lives here so it is unit-testable without
 * Android or GL. Zoom is always kept inside 0.1x-32x.
 */
data class ViewTransform(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun withPan(dx: Float, dy: Float): ViewTransform =
        copy(offsetX = offsetX + dx, offsetY = offsetY + dy)

    /**
     * Focus-preserving zoom: the surface point under ([focusX], [focusY])
     * stays put while [zoom] scales by [factor].
     */
    fun withZoom(factor: Float, focusX: Float, focusY: Float): ViewTransform {
        if (zoom <= 0f) return copy(zoom = GlCompositor.MIN_ZOOM)
        val newZoom = (zoom * factor).coerceIn(GlCompositor.MIN_ZOOM, GlCompositor.MAX_ZOOM)
        val scale = newZoom / zoom
        return copy(
            zoom = newZoom,
            offsetX = focusX - (focusX - offsetX) * scale,
            offsetY = focusY - (focusY - offsetY) * scale
        )
    }
}
