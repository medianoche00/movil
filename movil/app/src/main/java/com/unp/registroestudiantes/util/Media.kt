package com.unp.registroestudiantes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max

/*
 * Archivo: Media.kt
 * Proposito: Utilidades para optimizacion, rotacion y compresion de imagenes,
 *            y obtencion de nombres de archivos a partir de Content URIs.
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. Parametros con valores por defecto:
 *    'fun compressImage(..., maxSide: Int = 1024)'
 *    En Java necesitarias sobrecargar metodos (method overloading):
 *      public byte[] compressImage(Context ctx, Uri uri) { return compressImage(ctx, uri, 1024); }
 *      public byte[] compressImage(Context ctx, Uri uri, int maxSide) { ... }
 *    En Kotlin se declara una unica vez asignando el valor predeterminado.
 *
 * 2. Scope functions (.apply, .also, .use):
 *    - .apply { inJustDecodeBounds = true }: Ejecuta el bloque en el contexto del objeto
 *      y lo devuelve. Muy util para inicializar propiedades como un mini-builder.
 *    - .also { ... }: Ejecuta una accion secundaria con el objeto y retorna el objeto original.
 *    - .use { ... }: Cierra automaticamente el recurso al terminar el bloque (try-with-resources).
 *
 * 3. Downsampling (Reduccion de tamano en memoria):
 *    Las camaras de celulares modernos toman fotos de 12 a 50 Megapixeles (mas de 30 MB en RAM).
 *    Si intentas decodificar esa imagen en un Bitmap sin precauciones, la app sufrira
 *    un OutOfMemoryError (OOM) y se cerrara.
 *    El algoritmo lee solo las dimensiones (inJustDecodeBounds), calcula un factor
 *    de submuestreo potencial de 2 (inSampleSize: 1, 2, 4, 8...), y carga unicamente
 *    la resolucion necesaria para la aplicacion (maximo 1024px).
 *
 * 4. Metadatos EXIF y Correccion de Orientacion:
 *    Los sensores de camara en Android suelen almacenar la imagen en una orientacion fisica
 *    fija y guardan la orientacion de captura en los metadatos EXIF. Si no se lee la etiqueta
 *    ExifInterface.TAG_ORIENTATION, las fotos tomadas en vertical aparecen giradas 90 grados.
 */
object Media {

    /**
     * Lee una imagen desde un Uri, corrige su orientacion segun metadatos EXIF,
     * reduce su escala si supera los 1024px y la comprime a un arreglo de bytes JPEG.
     */
    fun compressImage(ctx: Context, uri: Uri, maxSide: Int = 1024): ByteArray? {
        // Paso 1: Lee unicamente los limites (ancho y alto) sin cargar pixeles en memoria
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        // Paso 2: Calcula el factor de reduccion en potencia de dos
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) {
            sample *= 2
        }

        // Paso 3: Decodifica la imagen aplicando el submuestreo
        var bmp = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        // Paso 4: Lee la orientacion EXIF para corregir rotaciones de camara
        val orientation = ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // Si la foto esta rotada, aplica la matriz de transformacion
        if (degrees != 0f) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        }

        // Paso 5: Comprime el bitmap final a formato JPEG
        return toJpeg(bmp)
    }

    /**
     * Comprime un objeto Bitmap a un arreglo binario (ByteArray) en formato JPEG con calidad del 85%.
     */
    fun toJpeg(bmp: Bitmap, quality: Int = 85): ByteArray =
        ByteArrayOutputStream().also { stream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }.toByteArray()

    /**
     * Consulta el proveedor de contenidos del sistema para recuperar el nombre real
     * de un archivo (por ejemplo, "mi_curriculum.pdf") a partir de su URI de seleccion.
     */
    fun displayName(ctx: Context, uri: Uri): String? =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
