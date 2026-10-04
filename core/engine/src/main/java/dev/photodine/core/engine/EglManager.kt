package dev.photodine.core.engine

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log

private const val TAG = "EglManager"
private const val EGL_OPENGL_ES3_BIT = 0x40
private const val PBUFFER_WIDTH = 16
private const val PBUFFER_HEIGHT = 16

/**
 * Minimal EGL14 wrapper for an OpenGL ES 3.0 context: RGBA8, no depth/stencil.
 *
 * Two surface modes: a 16x16 pbuffer for off-screen work (texture allocation,
 * startup capability check, unit-testable contexts) and a window surface
 * created from the `TextureView` `SurfaceTexture` for preview. Must only be
 * used on the engine GL thread.
 */
internal class EglManager {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var config: EGLConfig? = null
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var pbuffer: EGLSurface = EGL14.EGL_NO_SURFACE
    private var window: EGLSurface = EGL14.EGL_NO_SURFACE

    val hasContext: Boolean
        get() = context != EGL14.EGL_NO_CONTEXT

    val hasWindow: Boolean
        get() = window != EGL14.EGL_NO_SURFACE

    /** Creates display + ES3 context + pbuffer; idempotent. Throws on failure. */
    fun ensureContext() {
        if (hasContext) return
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
        config = chooseConfig()
        context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0
        )
        check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext (ES3) failed" }
        pbuffer = EGL14.eglCreatePbufferSurface(
            display, config,
            intArrayOf(EGL14.EGL_WIDTH, PBUFFER_WIDTH, EGL14.EGL_HEIGHT, PBUFFER_HEIGHT,
                EGL14.EGL_NONE), 0
        )
        check(pbuffer != EGL14.EGL_NO_SURFACE) { "eglCreatePbufferSurface failed" }
        makePbufferCurrent()
    }

    fun attachWindow(surface: SurfaceTexture) {
        ensureContext()
        if (hasWindow) detachWindow()
        window = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(window != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
        makeWindowCurrent()
    }

    fun detachWindow() {
        if (window == EGL14.EGL_NO_SURFACE) return
        makePbufferCurrent()
        EGL14.eglDestroySurface(display, window)
        window = EGL14.EGL_NO_SURFACE
    }

    fun makeWindowCurrent(): Boolean =
        EGL14.eglMakeCurrent(display, window, window, context)

    fun makePbufferCurrent(): Boolean =
        EGL14.eglMakeCurrent(display, pbuffer, pbuffer, context)

    fun swapWindow(): Boolean = EGL14.eglSwapBuffers(display, window)

    fun release() {
        if (display == EGL14.EGL_NO_DISPLAY) return
        runCatching {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
            )
            if (hasWindow) EGL14.eglDestroySurface(display, window)
            if (pbuffer != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, pbuffer)
            if (hasContext) EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }.onFailure { Log.w(TAG, "release failed", it) }
        window = EGL14.EGL_NO_SURFACE
        pbuffer = EGL14.EGL_NO_SURFACE
        context = EGL14.EGL_NO_CONTEXT
        display = EGL14.EGL_NO_DISPLAY
        config = null
    }

    private fun chooseConfig(): EGLConfig {
        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) && count[0] > 0) {
            "eglChooseConfig found no RGBA8 ES3 config"
        }
        return requireNotNull(configs[0])
    }
}
