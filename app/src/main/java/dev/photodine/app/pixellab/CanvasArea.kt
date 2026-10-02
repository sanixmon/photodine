package dev.photodine.app.pixellab

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun CanvasArea(
    layers: List<TextLayer>,
    selectedId: String?,
    showGrid: Boolean,
    onSelectLayer: (String) -> Unit,
    onClearSelection: () -> Unit,
    onTransformLayer: (id: String, offset: Offset, scale: Float, rotation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PixelLabCanvasBg)
            .pointerInput(Unit) {
                detectTapGestures { onClearSelection() }
            },
        contentAlignment = Alignment.Center
    ) {
        // Square Canvas (#666666)
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(PixelLabCanvasSurface)
                .drawBehind {
                    if (showGrid) {
                        val gridColor = Color.White.copy(alpha = 0.45f)
                        val strokeWidth = 1.dp.toPx()

                        // 3x3 grid: 2 vertical lines
                        val stepX = size.width / 3f
                        drawLine(gridColor, Offset(stepX, 0f), Offset(stepX, size.height), strokeWidth)
                        drawLine(gridColor, Offset(stepX * 2, 0f), Offset(stepX * 2, size.height), strokeWidth)

                        // 3x3 grid: 2 horizontal lines
                        val stepY = size.height / 3f
                        drawLine(gridColor, Offset(0f, stepY), Offset(size.width, stepY), strokeWidth)
                        drawLine(gridColor, Offset(0f, stepY * 2), Offset(size.width, stepY * 2), strokeWidth)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { onClearSelection() }
                },
            contentAlignment = Alignment.Center
        ) {
            layers.forEach { layer ->
                TextLayerItem(
                    layer = layer,
                    isSelected = layer.id == selectedId,
                    onSelect = onSelectLayer,
                    onTransform = { offset, scale, rot ->
                        onTransformLayer(layer.id, offset, scale, rot)
                    }
                )
            }
        }
    }
}
