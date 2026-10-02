package dev.photodine.feature.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportTest {

    @Test
    fun `export format extensions and mime types are valid`() {
        assertEquals("png", ExportFormat.PNG.extension)
        assertEquals("image/png", ExportFormat.PNG.mimeType)

        assertEquals("jpg", ExportFormat.JPEG.extension)
        assertEquals("image/jpeg", ExportFormat.JPEG.mimeType)
    }

    @Test
    fun `jpeg quality clamping behavior`() {
        val low = (-5).coerceIn(10, 100)
        assertEquals(10, low)

        val mid = 75.coerceIn(10, 100)
        assertEquals(75, mid)

        val high = 150.coerceIn(10, 100)
        assertEquals(100, high)
    }

    @Test
    fun `default export ui state has sensible defaults`() {
        val state = ExportUiState()
        assertEquals(ExportFormat.PNG, state.format)
        assertEquals(90, state.jpegQuality)
        assertEquals(false, state.isExporting)
    }
}
