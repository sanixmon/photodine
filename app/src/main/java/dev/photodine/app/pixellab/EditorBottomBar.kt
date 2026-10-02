package dev.photodine.app.pixellab

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorBottomBar(
    activeTab: EditorTab,
    onSelectTab: (EditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // HorizontalDivider biru tipis di atas
            HorizontalDivider(
                color = PixelLabBlue.copy(alpha = 0.45f),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val tabColor = if (isSelected) PixelLabBlueActive else PixelLabInactiveGray

                    val indicatorColor by animateColorAsState(
                        targetValue = if (isSelected) PixelLabBlueActive else Color.Transparent,
                        label = "TabUnderline"
                    )

                    Column(
                        modifier = Modifier
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Custom PixelLab icons
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (tab) {
                                EditorTab.Blend -> BlendTabIcon(color = tabColor)
                                EditorTab.Text -> TextTabIcon(color = tabColor)
                                EditorTab.Shapes -> ShapesTabIcon(color = tabColor)
                                EditorTab.Images -> ImagesTabIcon(color = tabColor)
                                EditorTab.Effects -> EffectsTabIcon(color = tabColor)
                            }
                        }

                        // Underline indicator
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(2.5.dp)
                                .background(indicatorColor, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlendTabIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val r = size.minDimension * 0.28f
        val stroke = Stroke(width = 1.6.dp.toPx())
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(color, r, Offset(cx - r * 0.5f, cy + r * 0.4f), style = stroke)
        drawCircle(color, r, Offset(cx + r * 0.5f, cy + r * 0.4f), style = stroke)
        drawCircle(color, r, Offset(cx, cy - r * 0.5f), style = stroke)
    }
}

@Composable
private fun TextTabIcon(color: Color) {
    Text(
        text = "A",
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun ShapesTabIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.1f)
            lineTo(w * 0.75f, h * 0.1f)
            lineTo(w * 0.95f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.9f)
            lineTo(w * 0.25f, h * 0.9f)
            lineTo(w * 0.05f, h * 0.5f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.8.dp.toPx()))
    }
}

@Composable
private fun ImagesTabIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val s = size.width * 0.58f
        val stroke = Stroke(width = 1.6.dp.toPx())
        drawRect(color, Offset(1f, 1f), Size(s, s), style = stroke)
        drawRect(color, Offset(size.width - s - 1f, size.height - s - 1f), Size(s, s), style = stroke)
    }
}

@Composable
private fun EffectsTabIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        // Diagonal wand
        drawLine(
            color = color,
            start = Offset(4f, size.height - 4f),
            end = Offset(size.width * 0.65f, size.height * 0.35f),
            strokeWidth = 2.dp.toPx()
        )
        // Wand tip
        drawCircle(color, 2.5.dp.toPx(), Offset(size.width * 0.65f, size.height * 0.35f))
        // 3 spark stars
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * 0.85f, size.height * 0.2f))
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * 0.65f, size.height * 0.1f))
        drawCircle(color, 1.4.dp.toPx(), Offset(size.width * 0.9f, size.height * 0.42f))
    }
}
