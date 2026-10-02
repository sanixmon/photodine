package dev.photodine.app.pixellab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2

@Composable
fun TextLayerItem(
    layer: TextLayer,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    onTransform: (offset: Offset, scale: Float, rotation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .graphicsLayer {
                translationX = layer.offset.x
                translationY = layer.offset.y
                rotationZ = layer.rotation
                scaleX = layer.scale
                scaleY = layer.scale
            }
            .pointerInput(layer.id) {
                detectTapGestures { onSelect(layer.id) }
            }
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotate ->
                    onTransform(
                        layer.offset + pan,
                        layer.scale * zoom,
                        layer.rotation + rotate
                    )
                }
            }
    ) {
        // Selection bounding box with white border & semi-transparent white background
        Box(
            modifier = Modifier
                .padding(14.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .border(1.dp, Color.White)
                            .background(Color.White.copy(alpha = 0.15f))
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = layer.text,
                color = layer.color,
                fontSize = layer.fontSize,
                fontWeight = FontWeight.Light,
                maxLines = 2
            )
        }

        // Selected Handles
        if (isSelected) {
            // Handle Kanan-Atas (Resize / Scale)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, 0) }
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, PixelLabBlue, CircleShape)
                    .pointerInput(layer.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scaleDelta = 1f + (dragAmount.x - dragAmount.y) / 200f
                            onTransform(
                                layer.offset,
                                layer.scale * scaleDelta,
                                layer.rotation
                            )
                        }
                    }
            )

            // Handle Kiri-Bawah (Scale)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(0, 0) }
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, PixelLabBlue, CircleShape)
                    .pointerInput(layer.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scaleDelta = 1f + (-dragAmount.x + dragAmount.y) / 200f
                            onTransform(
                                layer.offset,
                                layer.scale * scaleDelta,
                                layer.rotation
                            )
                        }
                    }
            )

            // Handle Rotasi: busur kecil putih (Canvas drawArc, bentuk senyum) di bawah tengah box
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 18.dp)
                    .pointerInput(layer.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val angleRad = atan2(dragAmount.y.toDouble(), dragAmount.x.toDouble())
                            val angleDeg = Math.toDegrees(angleRad).toFloat()
                            onTransform(
                                layer.offset,
                                layer.scale,
                                layer.rotation + (angleDeg * 0.1f)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    // Draw smile arc (busur senyum)
                    drawArc(
                        color = Color.White,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(2f, 2f),
                        size = Size(size.width - 4f, size.height - 4f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}
