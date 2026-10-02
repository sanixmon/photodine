package dev.photodine.core.engine

import android.graphics.SurfaceTexture
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GlCompositor"
private const val CHECKER_SIZE_PX = 16f
private const val FRAME_LOG_INTERVAL = 300
private const val FLATTEN_TIMEOUT_SECONDS = 5L

/**
 * GLES 3.0 [Compositor].
 *
 * Every GL call runs on the private "PhotodineGL" [HandlerThread]; UI-thread
 * callers only post work. Preview composites visible layers bottom-to-top using
 * ping-pong accumulator FBOs and cached fragment programs per [BlendMode], then
 * renders directly to the `TextureView` surface over a transparency checkerboard.
 */
@Singleton
class GlCompositor @Inject constructor() : Compositor {

    private val thread = HandlerThread("PhotodineGL").also { it.start() }
    private val glHandler = Handler(thread.looper)
    private val egl = EglManager()
    private val textures = GlTextureStore()

    @Volatile private var ready = false
    @Volatile private var capabilityValue: GlesCapability? = null
    @Volatile private var cpuFallbackValue = false
    @Volatile private var canvasW = 0
    @Volatile private var canvasH = 0
    @Volatile private var surfaceW = 0
    @Volatile private var surfaceH = 0
    @Volatile private var zoom = 1f
    @Volatile private var offsetX = 0f
    @Volatile private var offsetY = 0f
    @Volatile private var surfaceAttached = false

    private val layerList = mutableListOf<Layer>()
    private val layerLock = Any()

    override val layers: List<Layer>
        get() = synchronized(layerLock) { layerList.toList() }

    // GL-thread only state.
    private var displayProgram = 0
    private val blendPrograms = mutableMapOf<BlendMode, Int>()
    private var quadBuffer: FloatBuffer? = null
    private var frameCount = 0
    private var lastFpsLogMs = 0L

    private val frameCallback = Choreographer.FrameCallback { renderLoopFrame() }

    override fun isReady(): Boolean = ready
    override val capability: GlesCapability? get() = capabilityValue
    override val cpuFallback: Boolean get() = cpuFallbackValue
    override val canvasWidth: Int get() = canvasW
    override val canvasHeight: Int get() = canvasH

    override fun initialiseCanvas(width: Int, height: Int) {
        val size = CanvasSize(width, height)
        glHandler.post { initialiseOnGlThread(size) }
    }

    override fun attachSurface(surface: SurfaceTexture, width: Int, height: Int) {
        glHandler.post {
            runCatching {
                egl.attachWindow(surface)
            }.onFailure {
                Log.w(TAG, "attachWindow failed; preview unavailable", it)
                return@post
            }
            surfaceW = width
            surfaceH = height
            surfaceAttached = true
            startFrameLoop()
        }
    }

    override fun updateSurfaceSize(width: Int, height: Int) {
        glHandler.post {
            surfaceW = width
            surfaceH = height
        }
    }

    override fun detachSurface() {
        glHandler.post {
            surfaceAttached = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            egl.detachWindow()
        }
    }

    override fun setViewTransform(zoom: Float, offsetX: Float, offsetY: Float) {
        val clamped = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        glHandler.post {
            this.zoom = clamped
            this.offsetX = offsetX
            this.offsetY = offsetY
        }
    }

    override fun requestRender() {
        glHandler.post { startFrameLoop() }
    }

    override fun addLayer(layer: Layer, index: Int?) {
        synchronized(layerLock) {
            if (index == null || index !in 0..layerList.size) {
                layerList.add(layer)
            } else {
                layerList.add(index, layer)
            }
        }
        requestRender()
    }

    override fun addLayerFromBitmap(bitmap: android.graphics.Bitmap, name: String): Layer {
        val latch = CountDownLatch(1)
        var createdLayer: Layer? = null
        glHandler.post {
            try {
                if (!textures.isInitialised) return@post
                egl.makePbufferCurrent()
                val texId = textures.createLayerTextureFromBitmap(bitmap)
                val layer = Layer(
                    name = name,
                    textureId = texId
                )
                synchronized(layerLock) {
                    layerList.add(layer)
                }
                createdLayer = layer
            } finally {
                latch.countDown()
            }
        }
        latch.await(FLATTEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        requestRender()
        return createdLayer ?: Layer(textureId = 0, name = name)
    }

    override fun removeLayer(id: UUID) {
        var removedTexId: Int? = null
        synchronized(layerLock) {
            val idx = layerList.indexOfFirst { it.id == id }
            if (idx != -1) {
                val removed = layerList.removeAt(idx)
                removedTexId = removed.textureId
            }
        }
        removedTexId?.let { texId ->
            glHandler.post { textures.deleteLayerTexture(texId) }
        }
        requestRender()
    }

    override fun updateLayer(layer: Layer) {
        synchronized(layerLock) {
            val idx = layerList.indexOfFirst { it.id == layer.id }
            if (idx != -1) {
                layerList[idx] = layer
            }
        }
        requestRender()
    }

    override fun reorderLayers(fromIndex: Int, toIndex: Int) {
        synchronized(layerLock) {
            if (fromIndex in layerList.indices && toIndex in layerList.indices) {
                val item = layerList.removeAt(fromIndex)
                layerList.add(toIndex, item)
            }
        }
        requestRender()
    }

    override fun cropCanvas(cropRect: CropRect) {
        glHandler.post {
            if (!textures.isInitialised) return@post
            val newW = cropRect.width
            val newH = cropRect.height
            val oldW = canvasW
            val oldH = canvasH

            egl.makePbufferCurrent()

            // Crop each layer texture
            synchronized(layerLock) {
                val updated = layerList.map { layer ->
                    val newTex = createCroppedTexture(layer.textureId, oldW, oldH, cropRect)
                    textures.deleteLayerTexture(layer.textureId)
                    layer.copy(textureId = newTex)
                }
                layerList.clear()
                layerList.addAll(updated)
            }

            // Reallocate accumulators
            textures.initialise(newW, newH)
            canvasW = newW
            canvasH = newH

            // Recenter view
            zoom = 1f
            offsetX = 0f
            offsetY = 0f

            startFrameLoop()
        }
    }

    private fun createCroppedTexture(oldTex: Int, oldW: Int, oldH: Int, cropRect: CropRect): Int {
        val newW = cropRect.width
        val newH = cropRect.height
        val newTex = textures.createLayerTexture()

        val readFbo = textures.attachToFbo(oldTex)
        val drawFbo = textures.attachToFbo(newTex)

        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, readFbo)
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, drawFbo)

        val srcY0 = oldH - cropRect.bottom
        val srcY1 = oldH - cropRect.top

        GLES30.glBlitFramebuffer(
            cropRect.left, srcY0, cropRect.right, srcY1,
            0, 0, newW, newH,
            GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST
        )

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glDeleteFramebuffers(2, intArrayOf(readFbo, drawFbo), 0)
        return newTex
    }

    override fun flatten(width: Int, height: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        val latch = CountDownLatch(1)
        glHandler.post {
            try {
                if (!textures.isInitialised) return@post
                egl.makePbufferCurrent()
                val compositedTextureId = compositeVisibleLayers()
                // Read back from composited texture via FBO
                val readFbo = textures.attachToFbo(compositedTextureId)
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, readFbo)
                GLES30.glViewport(0, 0, width, height)
                buffer.position(0)
                GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
                buffer.position(0)
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                GLES30.glDeleteFramebuffers(1, intArrayOf(readFbo), 0)
            } finally {
                latch.countDown()
            }
        }
        check(latch.await(FLATTEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "flatten timed out" }
        return buffer
    }

    override fun release() {
        glHandler.post {
            surfaceAttached = false
            runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
            textures.release()
            if (displayProgram != 0) GLES30.glDeleteProgram(displayProgram)
            displayProgram = 0
            blendPrograms.values.forEach { GLES30.glDeleteProgram(it) }
            blendPrograms.clear()
            quadBuffer = null
            egl.release()
            ready = false
        }
        thread.quitSafely()
    }

    private fun initialiseOnGlThread(size: CanvasSize) {
        runCatching { egl.ensureContext() }.onFailure {
            capabilityValue = GlesCapability(null, isEs3 = false, cpuFallback = true)
            cpuFallbackValue = true
            Log.w(TAG, "GLES 3.0 context creation failed; CPU fallback flagged", it)
            return
        }
        egl.makePbufferCurrent()
        checkCapability()
        textures.initialise(size.width, size.height)

        if (displayProgram == 0) {
            displayProgram = GlUtils.createProgram(DISPLAY_VERTEX, DISPLAY_FRAGMENT)
        }
        compileBlendPrograms()

        if (quadBuffer == null) {
            quadBuffer = ByteBuffer
                .allocateDirect(QUAD_COORDS.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(QUAD_COORDS)
                    position(0)
                }
        }

        // Initialize default base layer
        val baseTex = textures.createLayerTexture()
        synchronized(layerLock) {
            layerList.clear()
            layerList.add(Layer(name = "Layer 1", textureId = baseTex))
        }

        canvasW = size.width
        canvasH = size.height
        offsetX = 0f
        offsetY = 0f
        ready = displayProgram != 0 && textures.isInitialised
    }

    private fun compileBlendPrograms() {
        for (mode in BlendMode.entries) {
            if (!blendPrograms.containsKey(mode)) {
                val fragSrc = getBlendFragmentSource(mode)
                val prog = GlUtils.createProgram(BLEND_VERTEX, fragSrc)
                blendPrograms[mode] = prog
            }
        }
    }

    private fun checkCapability() {
        val version = GLES30.glGetString(GLES30.GL_VERSION)
        val isEs3 = version != null && version.contains("OpenGL ES 3.")
        capabilityValue = GlesCapability(version, isEs3, cpuFallback = !isEs3)
        cpuFallbackValue = !isEs3
        if (!isEs3) {
            Log.w(
                TAG,
                "GLES 3.0 conformance check failed (GL_VERSION=$version); " +
                    "CPU-fallback flag set, preview running best-effort"
            )
        } else {
            Log.d(TAG, "GLES capability OK: $version")
        }
    }

    private fun startFrameLoop() {
        if (!surfaceAttached || !egl.hasWindow) return
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun renderLoopFrame() {
        if (!surfaceAttached || !egl.hasWindow) return
        renderFrame()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun renderFrame() {
        if (!ready || surfaceW <= 0 || surfaceH <= 0) return
        if (!egl.makeWindowCurrent()) {
            Log.w(TAG, "makeWindowCurrent failed, skipping frame")
            return
        }

        val finalTextureId = compositeVisibleLayers()

        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glClearColor(0.12f, 0.12f, 0.12f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawCanvasQuad(finalTextureId)
        egl.swapWindow()
        logFps()
    }

    /**
     * Composites all visible layers bottom-to-top using the ping-pong accumulator FBOs.
     * Returns the GL texture ID holding the final composited result.
     */
    private fun compositeVisibleLayers(): Int {
        val visibleLayers = synchronized(layerLock) {
            layerList.filter { it.visible }
        }
        val fbos = textures.framebufferIds
        val accumTexs = textures.accumulatorTextureIds
        if (fbos.size < 2 || accumTexs.size < 2) return 0

        // Clear accum 0 to transparent
        textures.clearFbo(fbos[0], canvasW, canvasH)

        if (visibleLayers.isEmpty()) {
            return accumTexs[0]
        }

        var readIdx = 0
        var writeIdx = 1

        val quad = quadBuffer ?: return accumTexs[0]

        for (layer in visibleLayers) {
            val prog = blendPrograms[layer.blendMode] ?: continue
            val targetFbo = fbos[writeIdx]

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, targetFbo)
            GLES30.glViewport(0, 0, canvasW, canvasH)
            GLES30.glUseProgram(prog)

            // Dst accumulator -> TEXTURE0
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, accumTexs[readIdx])
            setUniform1i(prog, "uDst", 0)

            // Src layer -> TEXTURE1
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.textureId)
            setUniform1i(prog, "uSrc", 1)

            setUniform1f(prog, "uOpacity", layer.opacity)
            val matrix = layer.transform.toMatrix()
            val matLoc = GLES30.glGetUniformLocation(prog, "uTransform")
            if (matLoc != -1) {
                GLES30.glUniformMatrix3fv(matLoc, 1, false, matrix, 0)
            }

            val posLoc = GLES30.glGetAttribLocation(prog, "aPos")
            GLES30.glEnableVertexAttribArray(posLoc)
            GLES30.glVertexAttribPointer(posLoc, 2, GLES30.GL_FLOAT, false, 0, quad)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            GLES30.glDisableVertexAttribArray(posLoc)

            // Swap read and write indices
            val tmp = readIdx
            readIdx = writeIdx
            writeIdx = tmp
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return accumTexs[readIdx]
    }

    private fun drawCanvasQuad(textureId: Int) {
        val program = displayProgram
        val quad = quadBuffer ?: return
        if (program == 0 || !textures.isInitialised) return
        val fit = minOf(surfaceW / canvasW.toFloat(), surfaceH / canvasH.toFloat())
        val totalScale = fit * zoom
        val originX = (surfaceW - canvasW * totalScale) / 2f + offsetX
        val originY = (surfaceH - canvasH * totalScale) / 2f + offsetY

        GLES30.glUseProgram(program)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        setUniform1i(program, "uLayer", 0)
        setUniform2f(program, "uCanvasSize", canvasW.toFloat(), canvasH.toFloat())
        setUniform2f(program, "uSurfaceSize", surfaceW.toFloat(), surfaceH.toFloat())
        setUniform1f(program, "uScale", totalScale)
        setUniform2f(program, "uOrigin", originX, originY)
        setUniform1f(program, "uCheckerSize", CHECKER_SIZE_PX)

        val posLoc = GLES30.glGetAttribLocation(program, "aPos")
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 2, GLES30.GL_FLOAT, false, 0, quad)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(posLoc)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    private fun setUniform1i(program: Int, name: String, value: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, name), value)
    }

    private fun setUniform1f(program: Int, name: String, value: Float) {
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, name), value)
    }

    private fun setUniform2f(program: Int, name: String, x: Float, y: Float) {
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, name), x, y)
    }

    private fun logFps() {
        frameCount++
        if (frameCount % FRAME_LOG_INTERVAL != 0) return
        val now = System.currentTimeMillis()
        if (lastFpsLogMs != 0L) {
            val fps = FRAME_LOG_INTERVAL * 1000f / (now - lastFpsLogMs)
            Log.d(TAG, "preview fps≈%.1f (%dx%d canvas)".format(fps, canvasW, canvasH))
        }
        lastFpsLogMs = now
    }

    companion object {
        const val MIN_ZOOM = 0.1f
        const val MAX_ZOOM = 32f

        private val QUAD_COORDS = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
        )

        private const val BLEND_VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
out vec2 vUV;
void main() {
    vUV = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
}"""

        private fun getBlendFragmentSource(mode: BlendMode): String {
            val formula = when (mode) {
                BlendMode.NORMAL -> "vec3 b = s.rgb;"
                BlendMode.MULTIPLY -> "vec3 b = d.rgb * s.rgb;"
                BlendMode.SCREEN -> "vec3 b = 1.0 - (1.0 - d.rgb) * (1.0 - s.rgb);"
                BlendMode.OVERLAY -> """vec3 b = vec3(
    (d.r < 0.5) ? (2.0 * d.r * s.r) : (1.0 - 2.0 * (1.0 - d.r) * (1.0 - s.r)),
    (d.g < 0.5) ? (2.0 * d.g * s.g) : (1.0 - 2.0 * (1.0 - d.g) * (1.0 - s.g)),
    (d.b < 0.5) ? (2.0 * d.b * s.b) : (1.0 - 2.0 * (1.0 - d.b) * (1.0 - s.b))
);"""
                BlendMode.DARKEN -> "vec3 b = min(d.rgb, s.rgb);"
                BlendMode.LIGHTEN -> "vec3 b = max(d.rgb, s.rgb);"
            }

            return """#version 300 es
precision mediump float;
uniform sampler2D uDst;
uniform sampler2D uSrc;
uniform float uOpacity;
uniform mat3 uTransform;
in vec2 vUV;
out vec4 outColor;

void main() {
    vec4 d = texture(uDst, vUV);
    vec3 srcPos = uTransform * vec3(vUV, 1.0);
    vec2 sUV = srcPos.xy;
    vec4 s = vec4(0.0);
    if (sUV.x >= 0.0 && sUV.x <= 1.0 && sUV.y >= 0.0 && sUV.y <= 1.0) {
        s = texture(uSrc, sUV);
    }
    float a = s.a * uOpacity;
    $formula
    float outA = a + d.a * (1.0 - a);
    vec3 outRgb = outA > 0.0 ? mix(d.rgb, b, a) : vec3(0.0);
    outColor = vec4(outRgb, outA);
}"""
        }

        private const val DISPLAY_VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
out vec2 vCanvasCoord;
out vec2 vScreenPixel;
uniform vec2 uCanvasSize;
uniform vec2 uSurfaceSize;
uniform float uScale;
uniform vec2 uOrigin;

void main() {
    vec2 screenPixel = (aPos * 0.5 + 0.5) * uSurfaceSize;
    vScreenPixel = screenPixel;
    vCanvasCoord = (screenPixel - uOrigin) / uScale;
    gl_Position = vec4(aPos, 0.0, 1.0);
}"""

        private const val DISPLAY_FRAGMENT = """#version 300 es
precision mediump float;
in vec2 vCanvasCoord;
in vec2 vScreenPixel;
out vec4 outColor;

uniform sampler2D uLayer;
uniform vec2 uCanvasSize;
uniform float uCheckerSize;

void main() {
    if (vCanvasCoord.x < 0.0 || vCanvasCoord.x >= uCanvasSize.x ||
        vCanvasCoord.y < 0.0 || vCanvasCoord.y >= uCanvasSize.y) {
        outColor = vec4(0.12, 0.12, 0.12, 1.0);
        return;
    }
    vec2 uv = vCanvasCoord / uCanvasSize;
    vec4 layer = texture(uLayer, uv);

    vec2 checkerCoord = floor(vScreenPixel / uCheckerSize);
    float check = mod(checkerCoord.x + checkerCoord.y, 2.0);
    vec3 checkerColor = check < 0.5 ? vec3(0.85) : vec3(0.70);

    vec3 blended = mix(checkerColor, layer.rgb, layer.a);
    outColor = vec4(blended, 1.0);
}"""
    }
}
