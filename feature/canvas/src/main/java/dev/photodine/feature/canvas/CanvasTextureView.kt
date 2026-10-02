package dev.photodine.feature.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView
import dev.photodine.core.engine.Compositor
import dev.photodine.feature.canvas.stroke.StrokePoint
import kotlin.math.hypot

/**
 * [TextureView] hosting the engine surface. Handles pinch-to-zoom,
 * pan gestures, and stylus/touch stroke capture for drawing tools.
 */
class CanvasTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr) {

    var compositor: Compositor? = null
    var onTransformChanged: ((ViewTransform) -> Unit)? = null
    var onSurfaceAvailable: (() -> Unit)? = null

    var isDrawingTool: Boolean = true
    var onStrokeBatch: ((List<StrokePoint>) -> Unit)? = null
    var onStrokeEnd: (() -> Unit)? = null
    var onCanvasTapped: ((canvasX: Float, canvasY: Float) -> Unit)? = null

    private var transform: ViewTransform = ViewTransform()
    private var panPointerId: Int = INVALID_POINTER
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastPinchDistance: Float = 0f

    private var strokeActive = false
    private var downX = 0f
    private var downY = 0f

    init {
        isOpaque = false
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                compositor?.attachSurface(surface, width, height)
                pushTransform()
                onSurfaceAvailable?.invoke()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                compositor?.updateSurfaceSize(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                compositor?.detachSurface()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
    }

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
        if (event.pointerCount >= 2) {
            // Two-finger gesture: pan or pinch-zoom
            if (strokeActive) {
                strokeActive = false
                onStrokeEnd?.invoke()
            }
            handleTwoFingerGesture(event)
            return true
        }

        // Single-finger: drawing or pan/tap
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                panPointerId = event.getPointerId(0)
                lastX = event.x
                lastY = event.y

                if (isDrawingTool && onStrokeBatch != null) {
                    strokeActive = true
                    val pt = toCanvasPoint(event.x, event.y, event.getAxisValue(MotionEvent.AXIS_PRESSURE))
                    onStrokeBatch?.invoke(listOf(pt))
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (strokeActive && onStrokeBatch != null) {
                    val batch = mutableListOf<StrokePoint>()
                    val hist = event.historySize
                    for (h in 0 until hist) {
                        val hx = event.getHistoricalX(h)
                        val hy = event.getHistoricalY(h)
                        val hp = event.getHistoricalAxisValue(MotionEvent.AXIS_PRESSURE, h)
                        batch.add(toCanvasPoint(hx, hy, hp))
                    }
                    val cp = event.getAxisValue(MotionEvent.AXIS_PRESSURE)
                    batch.add(toCanvasPoint(event.x, event.y, cp))
                    onStrokeBatch?.invoke(batch)
                } else if (!isDrawingTool) {
                    handlePan(event)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (strokeActive) {
                    strokeActive = false
                    onStrokeEnd?.invoke()
                }
                val dist = hypot(event.x - downX, event.y - downY)
                if (dist < 10f) {
                    val canvasPt = toCanvasPoint(event.x, event.y, 1f)
                    onCanvasTapped?.invoke(canvasPt.x, canvasPt.y)
                }
                panPointerId = INVALID_POINTER
            }

            MotionEvent.ACTION_CANCEL -> {
                if (strokeActive) {
                    strokeActive = false
                    onStrokeEnd?.invoke()
                }
                panPointerId = INVALID_POINTER
            }
        }
        return true
    }

    private fun toCanvasPoint(screenX: Float, screenY: Float, pressure: Float): StrokePoint {
        val cx = (screenX - transform.offsetX) / transform.zoom
        val cy = (screenY - transform.offsetY) / transform.zoom
        return StrokePoint(cx, cy, pressure)
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

    private fun handleTwoFingerGesture(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    lastPinchDistance = pinchDistance(event)
                    lastX = (event.getX(0) + event.getX(1)) / 2f
                    lastY = (event.getY(0) + event.getY(1)) / 2f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val distance = pinchDistance(event)
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f

                    var current = transform
                    if (lastPinchDistance > 0f && distance > 0f) {
                        val factor = distance / lastPinchDistance
                        current = current.withZoom(factor, midX, midY)
                    }

                    val dx = midX - lastX
                    val dy = midY - lastY
                    current = current.withPan(dx, dy)

                    lastPinchDistance = distance
                    lastX = midX
                    lastY = midY
                    emit(current)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                lastPinchDistance = 0f
            }
        }
    }

    private fun pinchDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx, dy)
    }

    companion object {
        private const val INVALID_POINTER = -1
    }
}
