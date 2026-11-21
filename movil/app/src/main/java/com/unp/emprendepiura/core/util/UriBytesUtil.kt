package com.unp.emprendepiura.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

/*
 * Archivo: UriBytesUtil.kt
 * Proposito: Utilidad ligera para comprimir imagenes seleccionadas desde la galeria
 *            antes de subirlas a Supabase Storage (productos, tiendas y comprobantes).
 */
object UriBytesUtil {

    fun comprimirImagenUri(context: Context, uri: Uri, maxSide: Int = 1080, quality: Int = 82): ByteArray? {
        return runCatching {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return rawBytes

            val maxDim = max(original.width, original.height)
            val scaled = if (maxDim > maxSide) {
                val factor = maxSide.toFloat() / maxDim.toFloat()
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * factor).toInt().coerceAtLeast(1),
                    (original.height * factor).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }

            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (scaled !== original) scaled.recycle()
                original.recycle()
                out.toByteArray()
            }
        }.getOrNull()
    }
}
