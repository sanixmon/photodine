package dev.photodine.feature.colorpicker

import kotlin.math.abs
import kotlin.math.roundToInt

object ColorMath {

    /**
     * Converts HSV values to RGB channels (0..255).
     * [h] is hue in 0f..360f.
     * [s] is saturation in 0f..1f.
     * [v] is value/brightness in 0f..1f.
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
        val normalizedH = ((h % 360f) + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedV = v.coerceIn(0f, 1f)

        val c = clampedV * clampedS
        val x = c * (1f - abs(((normalizedH / 60f) % 2f) - 1f))
        val m = clampedV - c

        val (rPrime, gPrime, bPrime) = when ((normalizedH / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)

        return Triple(r, g, b)
    }

    /**
     * Converts RGB channels (0..255) to HSV values.
     * Returns Triple(hue in 0f..360f, saturation in 0f..1f, value in 0f..1f).
     */
    fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rNorm = r.coerceIn(0, 255) / 255f
        val gNorm = g.coerceIn(0, 255) / 255f
        val bNorm = b.coerceIn(0, 255) / 255f

        val cMax = maxOf(rNorm, gNorm, bNorm)
        val cMin = minOf(rNorm, gNorm, bNorm)
        val delta = cMax - cMin

        val h = when {
            delta == 0f -> 0f
            cMax == rNorm -> 60f * (((gNorm - bNorm) / delta) % 6f)
            cMax == gNorm -> 60f * (((bNorm - rNorm) / delta) + 2f)
            else -> 60f * (((rNorm - gNorm) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val s = if (cMax == 0f) 0f else delta / cMax
        val v = cMax

        return Triple(h, s, v)
    }

    /**
     * Parses a 6-character hex string (e.g. "FF5722" or "#FF5722") to an ARGB integer with 0xFF alpha.
     * Returns null if invalid.
     */
    fun parseHexColor(hex: String): Int? {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        return try {
            val r = cleaned.substring(0, 2).toInt(16)
            val g = cleaned.substring(2, 4).toInt(16)
            val b = cleaned.substring(4, 6).toInt(16)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Formats RGB channels as a 6-character uppercase hex string (e.g. "FF5722").
     */
    fun formatHexColor(r: Int, g: Int, b: Int): String {
        return "%02X%02X%02X".format(
            r.coerceIn(0, 255),
            g.coerceIn(0, 255),
            b.coerceIn(0, 255)
        )
    }
}
