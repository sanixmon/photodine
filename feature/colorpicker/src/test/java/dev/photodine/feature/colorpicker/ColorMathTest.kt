package dev.photodine.feature.colorpicker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ColorMathTest {

    @Test
    fun `boundary colors round-trip between RGB and HSV`() {
        val testColors = listOf(
            Triple(255, 0, 0),     // Pure red
            Triple(0, 255, 0),     // Pure green
            Triple(0, 0, 255),     // Pure blue
            Triple(255, 255, 255), // White
            Triple(0, 0, 0),       // Black
            Triple(128, 128, 128)  // Mid-grey
        )

        for ((r, g, b) in testColors) {
            val (h, s, v) = ColorMath.rgbToHsv(r, g, b)
            val (rBack, gBack, bBack) = ColorMath.hsvToRgb(h, s, v)
            assertEquals(r, rBack, "Red channel mismatch for ($r, $g, $b)")
            assertEquals(g, gBack, "Green channel mismatch for ($r, $g, $b)")
            assertEquals(b, bBack, "Blue channel mismatch for ($r, $g, $b)")
        }
    }

    @Test
    fun `hex formatting produces 6-char uppercase hex string`() {
        assertEquals("FF0000", ColorMath.formatHexColor(255, 0, 0))
        assertEquals("00FF00", ColorMath.formatHexColor(0, 255, 0))
        assertEquals("0000FF", ColorMath.formatHexColor(0, 0, 255))
        assertEquals("FFFFFF", ColorMath.formatHexColor(255, 255, 255))
        assertEquals("000000", ColorMath.formatHexColor(0, 0, 0))
        assertEquals("FF5722", ColorMath.formatHexColor(255, 87, 34))
    }

    @Test
    fun `hex parser accepts valid strings with and without hash`() {
        val parsed1 = ColorMath.parseHexColor("FF0000")
        assertNotNull(parsed1)
        assertEquals(0xFFFF0000.toInt(), parsed1)

        val parsed2 = ColorMath.parseHexColor("#00FF00")
        assertNotNull(parsed2)
        assertEquals(0xFF00FF00.toInt(), parsed2)

        val parsed3 = ColorMath.parseHexColor("  #0000ff  ")
        assertNotNull(parsed3)
        assertEquals(0xFF0000FF.toInt(), parsed3)
    }

    @Test
    fun `hex parser rejects invalid strings`() {
        assertNull(ColorMath.parseHexColor(""))
        assertNull(ColorMath.parseHexColor("FFF"))
        assertNull(ColorMath.parseHexColor("GGGGGG"))
        assertNull(ColorMath.parseHexColor("12345"))
        assertNull(ColorMath.parseHexColor("1234567"))
    }

    @Test
    fun `recent colors ring buffer caps at 10 items`() {
        var state = ColorPickerState()
        for (i in 0 until 15) {
            val color = (0xFF shl 24) or (i shl 16)
            state = ColorPickerReducer.reduce(state, ColorPickerIntent.SelectRecent(color))
            state = ColorPickerReducer.reduce(state, ColorPickerIntent.ClosePicker)
        }
        assertEquals(10, state.recentColors.size)
    }
}
