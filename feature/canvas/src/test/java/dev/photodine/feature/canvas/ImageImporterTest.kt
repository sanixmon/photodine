package dev.photodine.feature.canvas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImageImporterTest {

    @Test
    fun `photo within 2048 is not downscaled`() {
        val (w, h) = ImageImporter.calculateDownscaledSize(1080, 1920)
        assertEquals(1080, w)
        assertEquals(1920, h)
    }

    @Test
    fun `wide photo over 2048 is scaled proportionally`() {
        val (w, h) = ImageImporter.calculateDownscaledSize(4096, 2048)
        assertEquals(2048, w)
        assertEquals(1024, h)
    }

    @Test
    fun `tall photo over 2048 is scaled proportionally`() {
        val (w, h) = ImageImporter.calculateDownscaledSize(3000, 6000)
        assertEquals(1024, w)
        assertEquals(2048, h)
    }

    @Test
    fun `square photo over 2048 scales to exactly 2048x2048`() {
        val (w, h) = ImageImporter.calculateDownscaledSize(3000, 3000)
        assertEquals(2048, w)
        assertEquals(2048, h)
    }

    @Test
    fun `minimum dimension is at least 1`() {
        val (w, h) = ImageImporter.calculateDownscaledSize(10000, 1)
        assertEquals(2048, w)
        assertTrue(h >= 1)
    }
}
