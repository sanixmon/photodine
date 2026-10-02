package dev.photodine.feature.colorpicker

data class ColorPickerState(
    val hue: Float = 0f,
    val saturation: Float = 1f,
    val value: Float = 1f,
    val alpha: Float = 1f,
    val red: Int = 255,
    val green: Int = 0,
    val blue: Int = 0,
    val hexString: String = "FF0000",
    val recentColors: List<Int> = emptyList(),
    val isEyedropperActive: Boolean = false
) {
    /** 32-bit packed ARGB integer. */
    val argbColor: Int
        get() {
            val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
            return (a shl 24) or (red shl 16) or (green shl 8) or blue
        }
}

sealed interface ColorPickerIntent {
    data class SetHsv(val h: Float, val s: Float, val v: Float) : ColorPickerIntent
    data class SetRgb(val r: Int, val g: Int, val b: Int) : ColorPickerIntent
    data class SetHex(val hex: String) : ColorPickerIntent
    data class SetAlpha(val alpha: Float) : ColorPickerIntent
    data class SelectRecent(val color: Int) : ColorPickerIntent
    data object ActivateEyedropper : ColorPickerIntent
    data class SampleEyedropperPixel(val color: Int) : ColorPickerIntent
    data object ClosePicker : ColorPickerIntent
}

object ColorPickerReducer {

    const val MAX_RECENTS = 10

    fun reduce(state: ColorPickerState, intent: ColorPickerIntent): ColorPickerState {
        return when (intent) {
            is ColorPickerIntent.SetHsv -> {
                val (r, g, b) = ColorMath.hsvToRgb(intent.h, intent.s, intent.v)
                state.copy(
                    hue = intent.h,
                    saturation = intent.s,
                    value = intent.v,
                    red = r,
                    green = g,
                    blue = b,
                    hexString = ColorMath.formatHexColor(r, g, b)
                )
            }

            is ColorPickerIntent.SetRgb -> {
                val (h, s, v) = ColorMath.rgbToHsv(intent.r, intent.g, intent.b)
                state.copy(
                    red = intent.r.coerceIn(0, 255),
                    green = intent.g.coerceIn(0, 255),
                    blue = intent.b.coerceIn(0, 255),
                    hue = h,
                    saturation = s,
                    value = v,
                    hexString = ColorMath.formatHexColor(intent.r, intent.g, intent.b)
                )
            }

            is ColorPickerIntent.SetHex -> {
                val parsed = ColorMath.parseHexColor(intent.hex)
                if (parsed != null) {
                    val r = (parsed ushr 16) and 0xFF
                    val g = (parsed ushr 8) and 0xFF
                    val b = parsed and 0xFF
                    val (h, s, v) = ColorMath.rgbToHsv(r, g, b)
                    state.copy(
                        red = r,
                        green = g,
                        blue = b,
                        hue = h,
                        saturation = s,
                        value = v,
                        hexString = intent.hex.trim().removePrefix("#").uppercase()
                    )
                } else {
                    state.copy(hexString = intent.hex)
                }
            }

            is ColorPickerIntent.SetAlpha -> state.copy(alpha = intent.alpha.coerceIn(0f, 1f))

            is ColorPickerIntent.SelectRecent -> {
                val r = (intent.color ushr 16) and 0xFF
                val g = (intent.color ushr 8) and 0xFF
                val b = intent.color and 0xFF
                val a = ((intent.color ushr 24) and 0xFF) / 255f
                val (h, s, v) = ColorMath.rgbToHsv(r, g, b)
                state.copy(
                    red = r,
                    green = g,
                    blue = b,
                    alpha = a,
                    hue = h,
                    saturation = s,
                    value = v,
                    hexString = ColorMath.formatHexColor(r, g, b)
                )
            }

            ColorPickerIntent.ActivateEyedropper -> state.copy(isEyedropperActive = true)

            is ColorPickerIntent.SampleEyedropperPixel -> {
                val r = (intent.color ushr 16) and 0xFF
                val g = (intent.color ushr 8) and 0xFF
                val b = intent.color and 0xFF
                val (h, s, v) = ColorMath.rgbToHsv(r, g, b)
                state.copy(
                    red = r,
                    green = g,
                    blue = b,
                    hue = h,
                    saturation = s,
                    value = v,
                    hexString = ColorMath.formatHexColor(r, g, b),
                    isEyedropperActive = false
                )
            }

            ColorPickerIntent.ClosePicker -> {
                val current = state.argbColor
                val updatedRecents = (listOf(current) + state.recentColors.filter { it != current })
                    .take(MAX_RECENTS)
                state.copy(recentColors = updatedRecents)
            }
        }
    }
}
