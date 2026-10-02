package dev.photodine.core.engine

import android.graphics.SurfaceTexture
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Public entry point to the GPU compositing engine and the primary test seam.
 *
 * Implementations run every GL call on a private render thread ("PhotodineGL");
 * all methods are safe to call from the UI thread and return immediately.
 * The `TextureView` `SurfaceTexture` is the final render target — there is
 * no intermediate CPU readback on the preview path.
 */
interface Compositor {
    fun isReady(): Boolean

    /** Startup capability-check outcome; null before the first GL context exists. */
    val capability: GlesCapability?

    /** True when the driver failed the GLES 3.0 check (warning is logged). */
    val cpuFallback: Boolean

    val canvasWidth: Int
    val canvasHeight: Int

    /** Ordered list of layers bottom-to-top. */
    val layers: List<Layer>

    /** Allocates textures and ping-pong FBO pair for a new blank canvas. */
    fun initialiseCanvas(width: Int, height: Int)

    /** Binds the `TextureView` surface as the render target and starts the frame loop. */
    fun attachSurface(surface: SurfaceTexture, width: Int, height: Int)

    fun updateSurfaceSize(width: Int, height: Int)

    /** Stops the frame loop and releases the window surface. */
    fun detachSurface()

    /**
     * Canvas view transform. [zoom] is clamped to 0.1x-32x; offsets are
     * surface-pixel coordinates of the canvas origin (top-left).
     */
    fun setViewTransform(zoom: Float, offsetX: Float, offsetY: Float)
    fun addLayer(layer: Layer, index: Int? = null)

    /**
     * Uploads [bitmap] to a new layer texture and inserts it as a Layer.
     */
    fun addLayerFromBitmap(bitmap: android.graphics.Bitmap, name: String = "Imported Photo"): Layer
    /** Requests one immediate frame (also used to kick the vsync loop). */
    fun requestRender()

    /** Appends a new blank layer or inserts at [index]. */
    fun addLayer(layer: Layer, index: Int? = null)

    /** Removes the layer with [id] and deletes its texture. */
    fun removeLayer(id: UUID)

    /** Updates properties (opacity, blendMode, visibility, transform) of a layer. */
    fun updateLayer(layer: Layer)

    /** Reorders layers from [fromIndex] to [toIndex]. */
    fun reorderLayers(fromIndex: Int, toIndex: Int)

    /**
     * Flattens all visible layers bottom-to-top into an off-screen FBO and
     * returns a ByteBuffer containing RGBA8 pixels. Blocks until GL thread completes.
     * Must NOT be called during interactive preview.
     */
    fun flatten(width: Int = canvasWidth, height: Int = canvasHeight): ByteBuffer

    /** Tears down GL resources and stops the render thread. */
    fun release()
}
