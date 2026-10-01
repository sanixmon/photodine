package dev.photodine.core.engine

import android.opengl.GLES30
import android.util.Log

private const val TAG = "GlTextureStore"

/**
 * GLES 3.0 [TextureStore]. Must only be used on the engine GL thread with a
 * current EGL context; [GlCompositor] guarantees both.
 *
 * Ticket 02 allocates a single transparent-black RGBA8 layer texture plus the
 * ping-pong accumulator pair. Blend programs per [BlendMode] arrive in later
 * tickets; the cached-program map lives in the compositor.
 */
class GlTextureStore : TextureStore {
    override val maxDimension: Int = CanvasSize.MAX_DIMENSION

    override var isInitialised: Boolean = false
        private set
    override var canvasWidth: Int = 0
        private set
    override var canvasHeight: Int = 0
        private set
    override var layerTextureId: Int = 0
        private set
    override var accumulatorTextureIds: IntArray = intArrayOf()
        private set

    private var framebufferIds: IntArray = intArrayOf()

    /** FBO bound to the layer texture; brush stamps render here in later tickets. */
    internal var layerFramebufferId: Int = 0
        private set

    override fun initialise(width: Int, height: Int) {
        val size = CanvasSize(width, height)
        if (isInitialised) release()
        layerTextureId = createRgbaTexture(size.width, size.height)
        layerFramebufferId = attachToFbo(layerTextureId)
        clearFbo(layerFramebufferId, size.width, size.height)
        val accumA = createRgbaTexture(size.width, size.height)
        val accumB = createRgbaTexture(size.width, size.height)
        accumulatorTextureIds = intArrayOf(accumA, accumB)
        framebufferIds = intArrayOf(
            attachToFbo(accumA),
            attachToFbo(accumB)
        )
        canvasWidth = size.width
        canvasHeight = size.height
        isInitialised = true
        Log.d(TAG, "initialised ${size.width}x${size.height}")
    }

    override fun release() {
        if (!isInitialised) return
        val textures = intArrayOf(layerTextureId, *accumulatorTextureIds)
        GLES30.glDeleteTextures(textures.size, textures, 0)
        val fbos = intArrayOf(layerFramebufferId, *framebufferIds)
        GLES30.glDeleteFramebuffers(fbos.size, fbos, 0)
        layerTextureId = 0
        layerFramebufferId = 0
        accumulatorTextureIds = intArrayOf()
        framebufferIds = intArrayOf()
        canvasWidth = 0
        canvasHeight = 0
        isInitialised = false
    }

    private fun clearFbo(fbo: Int, width: Int, height: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun createRgbaTexture(width: Int, height: Int): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ids[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return ids[0]
    }

    private fun attachToFbo(textureId: Int): Int {
        val ids = IntArray(1)
        GLES30.glGenFramebuffers(1, ids, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, ids[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, textureId, 0
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.w(TAG, "FBO incomplete: $status")
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return ids[0]
    }
}
