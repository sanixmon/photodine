package dev.photodine.feature.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.photodine.core.engine.LayerTransform
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual bounding box with 4 corner handles and a center crosshair
 * reflecting [layerTransform] on the canvas with [viewTransform].
 */
@Composable
fun MoveTransformOverlay(
    canvasWidth: Int,
    canvasHeight: Int,
    layerTransform: LayerTransform,
    viewTransform: ViewTransform,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val totalScale = viewTransform.zoom
        val ox = viewTransform.offsetX
        val oy = viewTransform.offsetY

        // 4 corners of layer quad in layer space: [0, canvasWidth] x [0, canvasHeight]
        val corners = listOf(
            Offset(0f, 0f),
            Offset(canvasWidth.toFloat(), 0f),
            Offset(canvasWidth.toFloat(), canvasHeight.toFloat()),
            Offset(0f, canvasHeight.toFloat())
        )

        val rad = Math.toRadians(layerTransform.rotationDeg.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val lScale = layerTransform.scale

        fun mapPoint(p: Offset): Offset {
            // Apply layer transform relative to canvas center
            val cx = canvasWidth / 2f
            val cy = canvasHeight / 2f
            val dx = p.x - cx
            val dy = p.y - cy

            val rx = (dx * cosA - dy * sinA) * lScale + cx + layerTransform.translateX
            val ry = (dx * sinA + dy * cosA) * lScale + cy + layerTransform.translateY

            // Apply view transform (canvas to screen)
            val sx = rx * totalScale + ox
            val sy = ry * totalScale + oy
            return Offset(sx, sy)
        }

        val mappedCorners = corners.map { mapPoint(it) }

        // Draw bounding box
        val path = Path().apply {
            moveTo(mappedCorners[0].x, mappedCorners[0].y)
            for (i in 1..3) {
                lineTo(mappedCorners[i].x, mappedCorners[i].y)
            }
            close()
        }
        drawPath(path, color = Color(0xFF2196F3), style = Stroke(width = 2f))

        // Draw corner handles
        val handleRadius = 8f
        mappedCorners.forEach { corner ->
            drawCircle(color = Color.White, radius = handleRadius, center = corner)
            drawCircle(color = Color(0xFF2196F3), radius = handleRadius, center = corner, style = Stroke(width = 2f))
        }

        // Draw center cross
        val center = mapPoint(Offset(canvasWidth / 2f, canvasHeight / 2f))
        val crossSize = 12f
        drawLine(
            color = Color(0xFF2196F3),
            start = Offset(center.x - crossSize, center.y),
            end = Offset(center.x + crossSize, center.y),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFF2196F3),
            start = Offset(center.x, center.y - crossSize),
            end = Offset(center.x, center.y + crossSize),
            strokeWidth = 2f
        )
    }
}
