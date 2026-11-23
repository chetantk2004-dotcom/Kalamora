package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCoverUtil {
    private const val MAX_DIM = 420
    private const val JPEG_QUALITY = 72
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    fun uriToCompactDataUri(context: Context, uri: Uri): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxSide = max(boundsOpts.outWidth, boundsOpts.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > MAX_DIM * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scale = MAX_DIM.toFloat() / max(decoded.width, decoded.height).coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            val bytes = out.toByteArray()
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUri = "$DATA_URI_PREFIX$encoded"
            if (dataUri.length <= 220000) dataUri else null
        } catch (_: Exception) {
            null
        }
    }

    fun isDataUri(value: String?): Boolean =
        !value.isNullOrBlank() && value.startsWith("data:image/")

    fun decodeDataUriToBitmap(dataUri: String): ImageBitmap? {
        return try {
            val base64Part = dataUri.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun rememberDecodedDataUri(dataUri: String?): ImageBitmap? {
    return remember(dataUri) {
        if (dataUri != null && ImageCoverUtil.isDataUri(dataUri)) {
            ImageCoverUtil.decodeDataUriToBitmap(dataUri)
        } else null
    }
}
