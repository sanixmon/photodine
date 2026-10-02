package dev.photodine.feature.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream
import java.nio.ByteBuffer

enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

object ExportManager {

    /**
     * Converts an RGBA8 [ByteBuffer] read from OpenGL into a correctly oriented [Bitmap].
     * Flips vertical orientation because OpenGL origin is at the bottom-left.
     */
    fun createBitmapFromGlBuffer(buffer: ByteBuffer, width: Int, height: Int, format: ExportFormat): Bitmap {
        buffer.position(0)
        val rawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        rawBitmap.copyPixelsFromBuffer(buffer)

        // Vertical flip matrix
        val matrix = Matrix().apply { preScale(1f, -1f) }
        val flipped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height, matrix, false)
        rawBitmap.recycle()

        return if (format == ExportFormat.JPEG) {
            // For JPEG, render over white background to avoid transparent black corruption
            val opaque = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(opaque)
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawBitmap(flipped, 0f, 0f, null)
            flipped.recycle()
            opaque
        } else {
            flipped
        }
    }

    /**
     * Saves the [bitmap] to MediaStore Pictures/Photodine (Scoped storage, API 29+).
     */
    fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int = 90
    ): Result<Uri> = runCatching {
        val resolver = context.contentResolver
        val filename = "photodine_${System.currentTimeMillis()}.${format.extension}"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photodine")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: error("Failed to insert MediaStore record")

        val compressFormat = if (format == ExportFormat.PNG) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }

        resolver.openOutputStream(uri)?.use { stream: OutputStream ->
            check(bitmap.compress(compressFormat, quality.coerceIn(10, 100), stream)) {
                "Bitmap compression failed"
            }
        } ?: error("Failed to open output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        uri
    }

    /**
     * Creates an [Intent.ACTION_SEND] for sharing the exported image.
     */
    fun createShareIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
