package dev.photodine.core.engine

/**
 * Public abstraction over GPU layer-texture storage.
 *
 * Implementations manage layer textures plus the ping-pong
 * accumulator FBO pair used by the bottom-to-top compositing passes.
 * Downstream modules (layers, tools, export) depend on this interface,
 * never on the GL implementation.
 *
 * Threading: all methods must be called on the engine GL thread
 * ("PhotodineGL"). [GlCompositor] is the only production caller and
 * confines every call there.
 */
interface TextureStore {
    /** Maximum texture extent per axis in pixels. */
    val maxDimension: Int

    val isInitialised: Boolean
    val canvasWidth: Int
    val canvasHeight: Int

    /** GL handles of the two ping-pong accumulator textures. */
    val accumulatorTextureIds: IntArray

    /** Allocates (or reallocates) accumulator textures/FBOs for [width]x[height]. */
    fun initialise(width: Int, height: Int)

    /** Allocates a new transparent RGBA8 layer texture of canvas dimensions and returns its GL handle. */
    fun createLayerTexture(): Int

    /** Allocates an RGBA8 layer texture initialized with the pixels of [bitmap]. */
    fun createLayerTextureFromBitmap(bitmap: android.graphics.Bitmap): Int

    /** Deletes the specified layer texture. */
    fun deleteLayerTexture(textureId: Int)

    /** Clears the specified layer texture to transparent (0, 0, 0, 0). */
    fun clearLayerTexture(textureId: Int)

    /** Deletes every GL object owned by this store. */
    fun release()
}
