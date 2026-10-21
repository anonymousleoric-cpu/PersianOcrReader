package com.sahandsoft.matnkhan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Image loading / preparation utilities.
 *
 * Everything here runs on Dispatchers.IO and is designed to be memory-safe:
 * large photos are decoded with inSampleSize and downscaled to a maximum
 * dimension of [MAX_DIMENSION] before OCR.
 */
object ImageUtils {

    private const val MAX_DIMENSION = 2048

    /** File the system camera writes into when using TakePicture + FileProvider. */
    fun cameraOutputFile(context: Context): File {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        return File(dir, "camera_capture.jpg")
    }

    /**
     * Copies a content [Uri] (Photo Picker result) into the app cache so the
     * same file can be used for preview and OCR. Returns null on failure
     * (cancelled, unreadable, zero bytes, unsupported format...).
     */
    fun copyUriToCache(context: Context, uri: Uri): File? = try {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val outFile = File(dir, "picked_image.jpg")
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            outFile.outputStream().use { output -> stream.copyTo(output) }
        }
        if (outFile.exists() && outFile.length() > 0) outFile else null
    } catch (t: Throwable) {
        null
    }

    /**
     * Decodes [file] safely, applies EXIF rotation, downscales and applies a
     * mild grayscale + contrast enhancement that helps Tesseract without
     * destroying details.
     */
    suspend fun loadForOcr(context: Context, file: File): Bitmap =
        withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            check(bounds.outWidth > 0 && bounds.outHeight > 0) {
                "تصویر خراب است یا فرمت آن پشتیبانی نمی‌شود."
            }

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
                ?: throw IllegalStateException("تصویر قابل خواندن نبود.")

            val rotated = applyExifRotation(decoded, file.absolutePath)
            val scaled = scaleDown(rotated, MAX_DIMENSION)
            enhanceForOcr(scaled)
        }

    private fun applyExifRotation(bitmap: Bitmap, path: String): Bitmap = try {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) bitmap else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true
        )
    } catch (t: Throwable) {
        bitmap
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val scale = maxDim.toFloat() / largest
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postScale(scale, scale) }, true
        )
    }

    private fun enhanceForOcr(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(
            source.width, source.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            val grayscale = ColorMatrix().apply { setSaturation(0f) }
            val contrast = ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, -32f,
                    0f, 1.2f, 0f, 0f, -32f,
                    0f, 0f, 1.2f, 0f, -32f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            grayscale.postConcat(contrast)
            colorFilter = ColorMatrixColorFilter(grayscale)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
