package dev.photodine.core.engine

/**
 * Public abstraction over GPU layer-texture storage.
 *
 * Implementations own one blank RGBA8 layer texture plus the ping-pong
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

    /** GL handle of the sole layer texture; 0 when not initialised. */
    val layerTextureId: Int

    /** GL handles of the two ping-pong accumulator textures. */
    val accumulatorTextureIds: IntArray

    /** Allocates (or reallocates) every texture/FBO for [width]x[height]. */
    fun initialise(width: Int, height: Int)

    /** Deletes every GL object owned by this store. */
    fun release()
}
