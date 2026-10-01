package dev.photodine.core.engine

import android.graphics.SurfaceTexture

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

    /** Allocates the layer texture + ping-pong FBO pair for a new blank canvas. */
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

    /** Requests one immediate frame (also used to kick the vsync loop). */
    fun requestRender()

    /** Tears down GL resources and stops the render thread. */
    fun release()
}
