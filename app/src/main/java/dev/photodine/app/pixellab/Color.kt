package dev.photodine.app.pixellab

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PixelLabBlue = Color(0xFF3575DD)
val PixelLabBlueDark = Color(0xFF2B5FC0)
val PixelLabBlueActive = Color(0xFF1E6FE8)
val PixelLabCanvasBg = Color(0xFFFAFAFA)
val PixelLabCanvasSurface = Color(0xFF666666)
val PixelLabToolbarBg = Color(0xFFF5F5F5)
val PixelLabInactiveGray = Color(0xFF757575)

private val PixelLabColorScheme = lightColorScheme(
    primary = PixelLabBlue,
    onPrimary = Color.White,
    primaryContainer = PixelLabBlueDark,
    onPrimaryContainer = Color.White,
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = PixelLabToolbarBg,
    onSurfaceVariant = PixelLabInactiveGray,
    background = PixelLabCanvasBg,
    onBackground = Color(0xFF1C1B1F),
    outline = Color(0xFFE0E0E0)
)

@Composable
fun PixelLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelLabColorScheme,
        content = content
    )
}
