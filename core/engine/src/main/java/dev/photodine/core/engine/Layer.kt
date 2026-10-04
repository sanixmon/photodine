package dev.photodine.core.engine

import java.util.UUID

/**
 * Single compositing layer.
 *
 * @param id Unique identifier for this layer.
 * @param name Display name of the layer (e.g. "Layer 1").
 * @param textureId OpenGL texture handle (RGBA8 GL_TEXTURE_2D).
 * @param blendMode Blend mode used to composite this layer over lower layers.
 * @param opacity Layer transparency in 0f..1f.
 * @param visible When false, skipped entirely during compositing.
 * @param transform Non-destructive 2D affine transform applied at composite time.
 */
data class Layer(
    val id: UUID = UUID.randomUUID(),
    val name: String = "Layer",
    val textureId: Int,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val transform: LayerTransform = LayerTransform()
) {
    init {
        require(opacity in 0f..1f) { "Layer opacity must be in 0f..1f, got $opacity" }
    }
}
