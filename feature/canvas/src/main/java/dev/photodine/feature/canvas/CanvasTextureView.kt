package dev.photodine.feature.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView
import dev.photodine.core.engine.Compositor
import kotlin.math.hypot

/**
 * `TextureView` hosted in Compose via `AndroidView`.
 *
 * Responsibilities: forward the `SurfaceTexture` lifecycle to the [Compositor]
 * (surface attach/detach drives the GL frame loop) and translate touch into
 * [ViewTransform] updates — one-finger drag pans, two-finger pinch zooms
 * (clamped 0.1x-32x). The UI thread never calls GL directly; every
 * [Compositor] method posts to the engine render thread.
 */
class CanvasTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr) {

    var compositor: Compositor? = null

    /** Fires on every gesture-driven change; the host mirrors it into MVI state. */
    var onTransformChanged: ((ViewTransform) -> Unit)? = null

    /** Fires when the GL surface is ready so MVI state can reflect engine readiness. */
    var onSurfaceAvailable: (() -> Unit)? = null

    private var transform = ViewTransform()

    private var panPointerId = INVALID_POINTER
    private var lastX = 0f
    private var lastY = 0f
    private var lastPinchDistance = 0f

    init {
        isOpaque = false
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                compositor?.attachSurface(surface, w, h)
                pushTransform()
                onSurfaceAvailable?.invoke()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {
                compositor?.updateSurfaceSize(w, h)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                compositor?.detachSurface()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
    }

    /**
     * Applies a transform coming from MVI state without re-emitting it.
     * Used so external state changes (e.g. reset) reach the GL thread.
     */
    fun updateTransform(next: ViewTransform) {
        transform = next
        pushTransform()
    }

    private fun pushTransform() {
        compositor?.setViewTransform(transform.zoom, transform.offsetX, transform.offsetY)
    }

    private fun emit(next: ViewTransform) {
        transform = next
        pushTransform()
        onTransformChanged?.invoke(next)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                panPointerId = event.getPointerId(0)
                lastX = event.x
                lastY = event.y
                lastPinchDistance = 0f
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    lastPinchDistance = pinchDistance(event)
                    panPointerId = INVALID_POINTER
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    handlePinch(event)
                } else {
                    handlePan(event)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Remaining finger becomes the pan pointer to avoid a jump.
                val leaving = event.getPointerId(event.actionIndex)
                if (leaving == panPointerId || event.pointerCount - 1 < 2) {
                    val remaining = (0 until event.pointerCount)
                        .firstOrNull { event.getPointerId(it) != leaving }
                    if (remaining != null) {
                        panPointerId = event.getPointerId(remaining)
                        lastX = event.getX(remaining)
                        lastY = event.getY(remaining)
                    } else {
                        panPointerId = INVALID_POINTER
                    }
                }
                lastPinchDistance = 0f
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                panPointerId = INVALID_POINTER
                lastPinchDistance = 0f
            }
        }
        return true
    }

    private fun handlePan(event: MotionEvent) {
        val index = event.findPointerIndex(panPointerId)
        if (index < 0) return
        val dx = event.getX(index) - lastX
        val dy = event.getY(index) - lastY
        lastX = event.getX(index)
        lastY = event.getY(index)
        if (dx != 0f || dy != 0f) emit(transform.withPan(dx, dy))
    }

    private fun handlePinch(event: MotionEvent) {
        val distance = pinchDistance(event)
        if (lastPinchDistance > 0f && distance > 0f) {
            val focusX = (event.getX(0) + event.getX(1)) / 2f
            val focusY = (event.getY(0) + event.getY(1)) / 2f
            emit(transform.withZoom(distance / lastPinchDistance, focusX, focusY))
        }
        lastPinchDistance = distance
    }

    private fun pinchDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return hypot(
            (event.getX(0) - event.getX(1)).toDouble(),
            (event.getY(0) - event.getY(1)).toDouble()
        ).toFloat()
    }

    private companion object {
        const val INVALID_POINTER = -1
    }
}
