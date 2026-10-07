package com.example.elbuensabor.utils.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ProductImageUtils {
    private const val MAX_DIMENSION = 640
    private const val MAX_BASE64_CHARS = 600_000

    suspend fun uriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = loadScaledBitmap(context, uri)
                    ?: throw IllegalArgumentException("No se pudo leer la imagen seleccionada.")

                var encoded = ""
                var quality = 72

                while (quality >= 38) {
                    val output = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    if (encoded.length <= MAX_BASE64_CHARS) break
                    quality -= 10
                }

                if (encoded.isBlank() || encoded.length > MAX_BASE64_CHARS) {
                    throw IllegalArgumentException(
                        "La imagen es demasiado pesada. Elige otra foto o una imagen de menor tamaño."
                    )
                }

                encoded
            }
        }

    fun base64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val largestSide = max(width, height)

                if (largestSide > MAX_DIMENSION) {
                    val scale = MAX_DIMENSION.toFloat() / largestSide.toFloat()
                    decoder.setTargetSize(
                        (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeLegacy(context, uri)
        }

        return resizeIfNeeded(bitmap)
    }

    private fun decodeLegacy(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_DIMENSION * 2 ||
            bounds.outHeight / sampleSize > MAX_DIMENSION * 2
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap?): Bitmap? {
        bitmap ?: return null
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= MAX_DIMENSION) return bitmap

        val scale = MAX_DIMENSION.toFloat() / largestSide.toFloat()
        val newWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
