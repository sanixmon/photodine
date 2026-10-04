package dev.photodine.core.engine

enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN;

    companion object {
        /**
         * Pure RGBA blend formula matching the GLSL fragment shaders.
         * Input channels are expected in 0f..1f.
         * Returns FloatArray(outR, outG, outB, outA).
         */
        fun composite(
            dstR: Float, dstG: Float, dstB: Float, dstA: Float,
            srcR: Float, srcG: Float, srcB: Float, srcA: Float,
            opacity: Float,
            mode: BlendMode
        ): FloatArray {
            val a = srcA * opacity
            val bR: Float
            val bG: Float
            val bB: Float
            when (mode) {
                NORMAL -> {
                    bR = srcR
                    bG = srcG
                    bB = srcB
                }
                MULTIPLY -> {
                    bR = dstR * srcR
                    bG = dstG * srcG
                    bB = dstB * srcB
                }
                SCREEN -> {
                    bR = 1f - (1f - dstR) * (1f - srcR)
                    bG = 1f - (1f - dstG) * (1f - srcG)
                    bB = 1f - (1f - dstB) * (1f - srcB)
                }
                OVERLAY -> {
                    bR = if (dstR < 0.5f) 2f * dstR * srcR else 1f - 2f * (1f - dstR) * (1f - srcR)
                    bG = if (dstG < 0.5f) 2f * dstG * srcG else 1f - 2f * (1f - dstG) * (1f - srcG)
                    bB = if (dstB < 0.5f) 2f * dstB * srcB else 1f - 2f * (1f - dstB) * (1f - srcB)
                }
                DARKEN -> {
                    bR = minOf(dstR, srcR)
                    bG = minOf(dstG, srcG)
                    bB = minOf(dstB, srcB)
                }
                LIGHTEN -> {
                    bR = maxOf(dstR, srcR)
                    bG = maxOf(dstG, srcG)
                    bB = maxOf(dstB, srcB)
                }
            }
            val outA = a + dstA * (1f - a)
            val outR = if (outA > 0f) dstR + (bR - dstR) * a else 0f
            val outG = if (outA > 0f) dstG + (bG - dstG) * a else 0f
            val outB = if (outA > 0f) dstB + (bB - dstB) * a else 0f
            return floatArrayOf(outR, outG, outB, outA)
        }
    }
}
