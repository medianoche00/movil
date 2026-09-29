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

/**
 * Extrae texto del CV:
 *  - PDF con texto  -> PdfBox-Android
 *  - PDF escaneado o imagen -> ML Kit (OCR en el dispositivo, gratis y offline)
 */
object CvTextExtractor {

    suspend fun extract(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val type = context.contentResolver.getType(uri).orEmpty()
            if (type == "application/pdf") extractPdf(context, uri) else extractImage(context, uri)
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun extractPdf(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc -> PDFTextStripper().getText(doc) }
        }.orEmpty()
        if (text.isNotBlank()) return text

        // Fallback: PDF sin texto seleccionable -> renderizar 2 primeras páginas y hacer OCR
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
        val sb = StringBuilder()
        pfd.use {
            PdfRenderer(it).use { renderer ->
                for (i in 0 until minOf(2, renderer.pageCount)) {
                    renderer.openPage(i).use { page ->
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

    private suspend fun extractImage(context: Context, uri: Uri): String {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return ""
        return ocr(bmp)
    }

    private suspend fun ocr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
            .addOnFailureListener { if (cont.isActive) cont.resume("") }
    }
}
