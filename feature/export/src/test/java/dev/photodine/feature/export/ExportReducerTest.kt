package dev.photodine.feature.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportReducerTest {

    private val initialState = ExportUiState()

    @Test
    fun `set format updates format`() {
        val next = ExportReducer.reduce(initialState, ExportIntent.SetFormat(ExportFormat.JPEG))
        assertEquals(ExportFormat.JPEG, next.format)
    }

    @Test
    fun `set quality clamps between 10 and 100`() {
        val normal = ExportReducer.reduce(initialState, ExportIntent.SetQuality(80))
        assertEquals(80, normal.jpegQuality)

        val low = ExportReducer.reduce(initialState, ExportIntent.SetQuality(2))
        assertEquals(10, low.jpegQuality)

        val high = ExportReducer.reduce(initialState, ExportIntent.SetQuality(150))
        assertEquals(100, high.jpegQuality)
    }

    @Test
    fun `export sets isExporting true`() {
        val next = ExportReducer.reduce(initialState, ExportIntent.Export(andShare = false))
        assertTrue(next.isExporting)
    }
}
