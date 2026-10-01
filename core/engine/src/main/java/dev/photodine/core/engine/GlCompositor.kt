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
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GlCompositor"
private const val CHECKER_SIZE_PX = 16f
private const val FRAME_LOG_INTERVAL = 300

/**
 * GLES 3.0 [Compositor].
 *
 * Every GL call runs on the private "PhotodineGL" [HandlerThread]; UI-thread
 * callers only post work. While a window surface is attached, frames are
 * produced by a vsync-driven [Choreographer] loop (~60fps idle) that draws
 * the single layer texture over a checkerboard directly into the
 * `TextureView` surface — no CPU readback. The ping-pong FBO pair owned by
 * [GlTextureStore] is allocated now; blend programs per [BlendMode] arrive
 * in later tickets.
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

    // GL-thread only state.
    private var displayProgram = 0
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

    override fun release() {
        glHandler.post {
            surfaceAttached = false
            runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
            textures.release()
            if (displayProgram != 0) GLES30.glDeleteProgram(displayProgram)
            displayProgram = 0
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
            quadBuffer = ByteBuffer
                .allocateDirect(QUAD_COORDS.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(QUAD_COORDS)
                    position(0)
                }
        }
        canvasW = size.width
        canvasH = size.height
        // Fit-to-screen zoom is derived per frame from surface/canvas sizes;
        // reset pan so a re-initialised canvas starts centred.
        offsetX = 0f
        offsetY = 0f
        ready = displayProgram != 0 && textures.isInitialised
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
        GLES30.glViewport(0, 0, surfaceW, surfaceH)
        GLES30.glClearColor(0.12f, 0.12f, 0.12f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawCanvasQuad()
        egl.swapWindow()
        logFps()
    }

    private fun drawCanvasQuad() {
        val program = displayProgram
        val quad = quadBuffer ?: return
        if (program == 0 || !textures.isInitialised) return
        val fit = minOf(surfaceW / canvasW.toFloat(), surfaceH / canvasH.toFloat())
        val totalScale = fit * zoom
        val originX = (surfaceW - canvasW * totalScale) / 2f + offsetX
        val originY = (surfaceH - canvasH * totalScale) / 2f + offsetY

        GLES30.glUseProgram(program)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.layerTextureId)
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
        /** Zoom range shared by the engine clamp and the canvas gesture layer. */
        const val MIN_ZOOM = 0.1f
        const val MAX_ZOOM = 32f

        private val QUAD_COORDS = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )

        /**
         * Trivial display program (passthrough + clear-level work only): maps the
         * canvas quad into the surface with fit-scale + pan offset. Per-[BlendMode]
         * compositing shaders arrive in later tickets.
         */
        private const val DISPLAY_VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
uniform vec2 uCanvasSize;
uniform vec2 uSurfaceSize;
uniform float uScale;
uniform vec2 uOrigin;
out vec2 vUV;
void main() {
    vec2 px = aPos * uCanvasSize * uScale + uOrigin;
    vec2 ndc = px / uSurfaceSize * vec2(2.0, -2.0) + vec2(-1.0, 1.0);
    gl_Position = vec4(ndc, 0.0, 1.0);
    vUV = aPos;
}
"""

        /**
         * Checkerboard transparency background with the blank layer texture
         * alpha-blended over it. UV convention: v=(0,0) is the canvas top-left,
         * matching `GLUtils.texImage2D` bitmap upload order used by import/export.
         */
        private const val DISPLAY_FRAGMENT = """#version 300 es
precision mediump float;
uniform sampler2D uLayer;
uniform vec2 uCanvasSize;
uniform float uCheckerSize;
in vec2 vUV;
out vec4 outColor;
void main() {
    vec2 cell = floor(vUV * uCanvasSize / uCheckerSize);
    float m = mod(cell.x + cell.y, 2.0);
    vec3 bg = mix(vec3(0.78), vec3(0.55), m);
    vec4 layer = texture(uLayer, vUV);
    outColor = vec4(mix(bg, layer.rgb, layer.a), 1.0);
}
"""
    }
}
