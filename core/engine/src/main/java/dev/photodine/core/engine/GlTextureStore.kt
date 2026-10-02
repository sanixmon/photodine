package dev.photodine.core.engine

import android.opengl.GLES30
import android.util.Log

private const val TAG = "GlTextureStore"

/**
 * GLES 3.0 [TextureStore]. Must only be used on the engine GL thread with a
 * current EGL context; [GlCompositor] guarantees both.
 *
 * Allocates layer textures plus the ping-pong accumulator pair.
 */
class GlTextureStore : TextureStore {
    override val maxDimension: Int = CanvasSize.MAX_DIMENSION

    override var isInitialised: Boolean = false
        private set
    override var canvasWidth: Int = 0
        private set
    override var canvasHeight: Int = 0
        private set

    override var accumulatorTextureIds: IntArray = intArrayOf()
        private set

    internal var framebufferIds: IntArray = intArrayOf()
        private set

    private val allocatedLayerTextures = mutableSetOf<Int>()
    private val textureFboMap = mutableMapOf<Int, Int>()

    override fun initialise(width: Int, height: Int) {
        val size = CanvasSize(width, height)
        if (isInitialised) release()
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

    override fun createLayerTexture(): Int {
        check(isInitialised) { "GlTextureStore not initialised" }
        val texId = createRgbaTexture(canvasWidth, canvasHeight)
        val fboId = attachToFbo(texId)
        clearFbo(fboId, canvasWidth, canvasHeight)
        allocatedLayerTextures.add(texId)
        textureFboMap[texId] = fboId
        return texId
    }

    override fun createLayerTextureFromBitmap(bitmap: android.graphics.Bitmap): Int {
        check(isInitialised) { "GlTextureStore not initialised" }
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, ids[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        android.opengl.GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        val fboId = attachToFbo(ids[0])
        allocatedLayerTextures.add(ids[0])
        textureFboMap[ids[0]] = fboId
        return ids[0]
    }

    fun getFramebufferForTexture(textureId: Int): Int {
        return textureFboMap.getOrPut(textureId) {
            attachToFbo(textureId)
        }
    }

    override fun deleteLayerTexture(textureId: Int) {
        if (allocatedLayerTextures.remove(textureId)) {
            val fbo = textureFboMap.remove(textureId)
            if (fbo != null) {
                GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            }
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
        }
    }

    override fun clearLayerTexture(textureId: Int) {
        val fbo = getFramebufferForTexture(textureId)
        clearFbo(fbo, canvasWidth, canvasHeight)
    }

    override fun release() {
        if (!isInitialised) return
        for (texId in allocatedLayerTextures.toList()) {
            deleteLayerTexture(texId)
        }
        allocatedLayerTextures.clear()
        textureFboMap.clear()

        if (accumulatorTextureIds.isNotEmpty()) {
            GLES30.glDeleteTextures(accumulatorTextureIds.size, accumulatorTextureIds, 0)
            accumulatorTextureIds = intArrayOf()
        }
        if (framebufferIds.isNotEmpty()) {
            GLES30.glDeleteFramebuffers(framebufferIds.size, framebufferIds, 0)
            framebufferIds = intArrayOf()
        }
        canvasWidth = 0
        canvasHeight = 0
        isInitialised = false
    }

    fun clearFbo(fbo: Int, width: Int, height: Int) {
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
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return ids[0]
    }

    fun attachToFbo(textureId: Int): Int {
        val ids = IntArray(1)
        GLES30.glGenFramebuffers(1, ids, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, ids[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, textureId, 0
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
            "FBO incomplete: 0x${status.toString(16)}"
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        return ids[0]
    }
}
