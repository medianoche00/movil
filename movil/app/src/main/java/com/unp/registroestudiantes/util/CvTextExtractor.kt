package com.unp.registroestudiantes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/*
 * Archivo: CvTextExtractor.kt
 * Proposito: Extraccion de texto a partir de archivos de CV (PDFs o imagenes).
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. Estrategia de procesamiento con Fallback:
 *    - Caso A: El usuario sube un PDF con texto digital seleccionable -> Se lee
 *      directamente mediante PdfBox-Android (rapido y 100% fiel al texto original).
 *    - Caso B: El usuario sube un PDF escaneado (fotocopia) -> Se renderizan las dos
 *      primeras paginas en imagenes Bitmap en memoria y se procesan con OCR.
 *    - Caso C: El usuario sube una foto o captura de su CV -> Se decodifica la imagen
 *      y se ejecuta ML Kit Text Recognition localmente en el dispositivo.
 *
 * 2. Gestion de recursos con la funcion '.use { ... }':
 *    En Java 7+ se utiliza el bloque 'try-with-resources':
 *      try (InputStream input = ...) { ... }
 *    En Kotlin, la funcion de extension '.use { ... }' implementa exactamente el mismo
 *    comportamiento para cualquier objeto que implemente AutoCloseable o Closeable,
 *    garantizando que streams y archivos se cierren incluso si ocurre una excepcion.
 *
 * 3. Integracion de APIs asincronas basadas en Callbacks con Corrutinas:
 *    ML Kit utiliza listeners asincronos (addOnSuccessListener, addOnFailureListener).
 *    Para evitar el clasico 'Callback Hell', Kotlin provee la funcion:
 *      suspendCancellableCoroutine { continuation -> ... }
 *    Esta funcion pausa la corrutina hasta que el listener responde y la reanuda
 *    llamando a 'continuation.resume(resultado)', permitiendo que el llamador reciba
 *    el valor como un retorno directo.
 */
object CvTextExtractor {

    /**
     * Extrae todo el contenido textual legible de un archivo referenciado por su Uri.
     * Se ejecuta en el despachador de hilos de E/S (Dispatchers.IO).
     */
    suspend fun extract(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val type = context.contentResolver.getType(uri).orEmpty()
            if (type == "application/pdf") {
                extractPdf(context, uri)
            } else {
                extractImage(context, uri)
            }
        } catch (e: Exception) {
            // Si ocurre un error de lectura, retorna una cadena vacia de forma segura
            ""
        }
    }

    /**
     * Extrae texto de un archivo PDF usando PdfBox-Android.
     * Si el PDF es un escaneo sin texto incrustado, ejecuta un fallback renderizando
     * paginas a Bitmap y aplicando OCR con Google ML Kit.
     */
    private suspend fun extractPdf(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)

        // Intento 1: Extraccion de texto digital incrustado
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                PDFTextStripper().getText(doc)
            }
        }.orEmpty()

        if (text.isNotBlank()) return text

        // Intento 2 (Fallback): Renderizado a imagen y reconocimiento optico de caracteres (OCR)
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
        val sb = StringBuilder()

        pfd.use {
            PdfRenderer(it).use { renderer ->
                val maxPages = minOf(2, renderer.pageCount)
                for (i in 0 until maxPages) {
                    renderer.openPage(i).use { page ->
                        // Crea un bitmap a 2x de resolucion para mejorar la precision del OCR
                        val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        sb.append(ocr(bmp)).append('\n')
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Decodifica una imagen desde el almacenamiento del dispositivo y le aplica OCR.
     */
    private suspend fun extractImage(context: Context, uri: Uri): String {
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return ""
        return ocr(bmp)
    }

    /**
     * Aplica reconocimiento de texto con Google ML Kit Vision directamente en el dispositivo.
     * Transforma la API con callbacks en una funcion suspendible secuencial.
     */
    private suspend fun ocr(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        client.process(image)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    continuation.resume(visionText.text)
                }
            }
            .addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume("")
                }
            }
    }
}
