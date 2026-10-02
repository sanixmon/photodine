package dev.photodine.feature.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.photodine.core.engine.CropRect

/**
 * Interactive crop overlay: dark vignette outside the crop box,
 * 4 draggable corner handles, and Confirm / Cancel buttons.
 */
@Composable
fun CropOverlay(
    canvasWidth: Int,
    canvasHeight: Int,
    viewTransform: ViewTransform,
    onConfirmCrop: (CropRect) -> Unit,
    onCancelCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Crop box coordinates in canvas pixels [0..canvasWidth] x [0..canvasHeight]
    var cropLeft by remember(canvasWidth, canvasHeight) { mutableFloatStateOf(0f) }
    var cropTop by remember(canvasWidth, canvasHeight) { mutableFloatStateOf(0f) }
    var cropRight by remember(canvasWidth, canvasHeight) { mutableFloatStateOf(canvasWidth.toFloat()) }
    var cropBottom by remember(canvasWidth, canvasHeight) { mutableFloatStateOf(canvasHeight.toFloat()) }

    val minCrop = CropRect.MIN_CROP_DIMENSION.toFloat()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canvasWidth, canvasHeight, viewTransform) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val scale = viewTransform.zoom
                        val canvasDx = dragAmount.x / scale
                        val canvasDy = dragAmount.y / scale

                        val touchCanvasX = (change.position.x - viewTransform.offsetX) / scale
                        val touchCanvasY = (change.position.y - viewTransform.offsetY) / scale

                        val dTopLeft = (touchCanvasX - cropLeft) * (touchCanvasX - cropLeft) +
                            (touchCanvasY - cropTop) * (touchCanvasY - cropTop)
                        val dTopRight = (touchCanvasX - cropRight) * (touchCanvasX - cropRight) +
                            (touchCanvasY - cropTop) * (touchCanvasY - cropTop)
                        val dBottomLeft = (touchCanvasX - cropLeft) * (touchCanvasX - cropLeft) +
                            (touchCanvasY - cropBottom) * (touchCanvasY - cropBottom)
                        val dBottomRight = (touchCanvasX - cropRight) * (touchCanvasX - cropRight) +
                            (touchCanvasY - cropBottom) * (touchCanvasY - cropBottom)

                        val minDist = minOf(dTopLeft, dTopRight, dBottomLeft, dBottomRight)

                        when (minDist) {
                            dTopLeft -> {
                                cropLeft = (cropLeft + canvasDx).coerceIn(0f, cropRight - minCrop)
                                cropTop = (cropTop + canvasDy).coerceIn(0f, cropBottom - minCrop)
                            }
                            dTopRight -> {
                                cropRight = (cropRight + canvasDx).coerceIn(cropLeft + minCrop, canvasWidth.toFloat())
                                cropTop = (cropTop + canvasDy).coerceIn(0f, cropBottom - minCrop)
                            }
                            dBottomLeft -> {
                                cropLeft = (cropLeft + canvasDx).coerceIn(0f, cropRight - minCrop)
                                cropBottom = (cropBottom + canvasDy).coerceIn(cropTop + minCrop, canvasHeight.toFloat())
                            }
                            dBottomRight -> {
                                cropRight = (cropRight + canvasDx).coerceIn(cropLeft + minCrop, canvasWidth.toFloat())
                                cropBottom = (cropBottom + canvasDy).coerceIn(cropTop + minCrop, canvasHeight.toFloat())
                            }
                        }
                    }
                }
        ) {
            val scale = viewTransform.zoom
            val ox = viewTransform.offsetX
            val oy = viewTransform.offsetY

            val screenLeft = cropLeft * scale + ox
            val screenTop = cropTop * scale + oy
            val screenRight = cropRight * scale + ox
            val screenBottom = cropBottom * scale + oy
            val screenW = screenRight - screenLeft
            val screenH = screenBottom - screenTop

            // 1. Dark vignette outside the crop box
            val vignetteColor = Color(0xAA000000)
            // Top
            drawRect(vignetteColor, Offset(0f, 0f), Size(size.width, screenTop))
            // Bottom
            drawRect(vignetteColor, Offset(0f, screenBottom), Size(size.width, size.height - screenBottom))
            // Left
            drawRect(vignetteColor, Offset(0f, screenTop), Size(screenLeft, screenH))
            // Right
            drawRect(vignetteColor, Offset(screenRight, screenTop), Size(size.width - screenRight, screenH))

            // 2. Crop border
            drawRect(
                color = Color.White,
                topLeft = Offset(screenLeft, screenTop),
                size = Size(screenW, screenH),
                style = Stroke(width = 2f)
            )

            // 3. Four corner handles
            val handleLen = 24f
            val handleThickness = 4f
            val handleColor = Color.White

            fun drawCorner(x: Float, y: Float, dirX: Float, dirY: Float) {
                drawLine(handleColor, Offset(x, y), Offset(x + dirX * handleLen, y), handleThickness)
                drawLine(handleColor, Offset(x, y), Offset(x, y + dirY * handleLen), handleThickness)
            }

            drawCorner(screenLeft, screenTop, 1f, 1f)
            drawCorner(screenRight, screenTop, -1f, 1f)
            drawCorner(screenLeft, screenBottom, 1f, -1f)
            drawCorner(screenRight, screenBottom, -1f, -1f)
        }

        // Action buttons: Confirm and Cancel
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancelCrop,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancel ✗")
            }

            Button(
                onClick = {
                    onConfirmCrop(
                        CropRect(
                            cropLeft.toInt(),
                            cropTop.toInt(),
                            cropRight.toInt(),
                            cropBottom.toInt()
                        )
                    )
                }
            ) {
                Text("Confirm ✓")
            }
        }
    }
}
