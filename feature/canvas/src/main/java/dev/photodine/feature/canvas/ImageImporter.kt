package dev.photodine.feature.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageImporter {

    const val MAX_IMAGE_DIMENSION = 2048

    /**
     * Decodes an image from [uri] using [ImageDecoder] (API 29+).
     * Proportinally downscales the image if either axis exceeds [maxDimension].
     * Preserves EXIF orientation.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun decodeBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_IMAGE_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val size = info.size
                val maxDim = maxOf(size.width, size.height)
                if (maxDim > maxDimension) {
                    val scale = maxDimension.toFloat() / maxDim
                    val targetW = (size.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (size.height * scale).toInt().coerceAtLeast(1)
                    decoder.setTargetSize(targetW, targetH)
                }
            }
        }.getOrNull()
    }

    /**
     * Calculates the downscaled dimensions preserving aspect ratio.
     */
    fun calculateDownscaledSize(width: Int, height: Int, maxDim: Int = MAX_IMAGE_DIMENSION): Pair<Int, Int> {
        val currentMax = maxOf(width, height)
        if (currentMax <= maxDim) return Pair(width, height)
        val scale = maxDim.toFloat() / currentMax
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Pair(targetW, targetH)
    }
}
