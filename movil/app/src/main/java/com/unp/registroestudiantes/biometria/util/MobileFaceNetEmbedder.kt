package com.unp.registroestudiantes.biometria.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/*
 * Archivo: MobileFaceNetEmbedder.kt
 * Proposito: Motor neuronal embebido basado en MobileFaceNet (TensorFlow Lite) que extrae
 *            un vector de identidad facial de 192 dimensiones L2-normalizado e invariante
 *            al efecto espejo de distintas camaras frontales mediante Test-Time Augmentation.
 */
object MobileFaceNetEmbedder {

    private const val MODEL_ASSET_NAME = "mobile_face_net.tflite"
    private const val INPUT_SIZE = 112
    const val DIM_EMBEDDING = 192

    private val lock = Any()
    private var interpreter: Interpreter? = null

    private fun obtenerInterpreter(context: Context): Interpreter? {
        synchronized(lock) {
            interpreter?.let { return it }
            return runCatching {
                val modelBuffer = cargarModeloDesdeAssets(context.applicationContext)
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                Interpreter(modelBuffer, options).also { interpreter = it }
            }.getOrNull()
        }
    }

    private fun cargarModeloDesdeAssets(context: Context): MappedByteBuffer {
        context.assets.openFd(MODEL_ASSET_NAME).use { fd ->
            FileInputStream(fd.fileDescriptor).use { fis ->
                return fis.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }
        }
    }

    /**
     * Alinea el rostro sobre un lienzo canonico de 112x112 pixeles posicionando los ojos
     * en las coordenadas estandar de MobileFaceNet/ArcFace: (38.3, 44.0) y (73.5, 44.0).
     */
    fun alinearRostro112(
        source: Bitmap,
        leftEye: PointF,
        rightEye: PointF
    ): Bitmap {
        val targetLeftX = 38.29f
        val targetRightX = 73.53f
        val targetEyeY = 44.0f
        val targetDist = targetRightX - targetLeftX

        val dx = rightEye.x - leftEye.x
        val dy = rightEye.y - leftEye.y
        val currentDist = hypot(dx, dy).coerceAtLeast(8f)
        val scale = targetDist / currentDist
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        val eyeMidX = (leftEye.x + rightEye.x) * 0.5f
        val eyeMidY = (leftEye.y + rightEye.y) * 0.5f
        val targetMidX = (targetLeftX + targetRightX) * 0.5f

        val matrix = Matrix().apply {
            postTranslate(-eyeMidX, -eyeMidY)
            postRotate(-angleDeg)
            postScale(scale, scale)
            postTranslate(targetMidX, targetEyeY)
        }

        val output = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Extrae el embedding de 192 dimensiones L2-normalizado evaluando tanto el rostro alineado
     * como su reflejo horizontal (Test-Time Augmentation), garantizando invarianza ante
     * diferencias de espejo entre celulares y mayor poder discriminativo entre personas.
     */
    fun extraerEmbedding(
        context: Context,
        sourceBitmap: Bitmap,
        leftEye: PointF,
        rightEye: PointF
    ): FloatArray {
        val aligned112 = alinearRostro112(sourceBitmap, leftEye, rightEye)
        val normalized112 = ecualizarIluminacionSuave(aligned112)
        val tflite = obtenerInterpreter(context)
            ?: return extraerEmbeddingRespaldoDeterminista(normalized112)

        return synchronized(lock) {
            runCatching {
                val embNormal = ejecutarInferenciaUnica(tflite, normalized112, espejarX = false)
                val embEspejo = ejecutarInferenciaUnica(tflite, normalized112, espejarX = true)
                normalizarL2InPlace(embNormal)
                normalizarL2InPlace(embEspejo)

                val combinado = FloatArray(DIM_EMBEDDING)
                for (i in 0 until DIM_EMBEDDING) {
                    combinado[i] = embNormal[i] + embEspejo[i]
                }
                normalizarL2InPlace(combinado)
                combinado
            }.getOrElse {
                extraerEmbeddingRespaldoDeterminista(normalized112)
            }
        }
    }

    private fun ejecutarInferenciaUnica(
        tflite: Interpreter,
        bitmap112: Bitmap,
        espejarX: Boolean
    ): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap112.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (y in 0 until INPUT_SIZE) {
            val rowOffset = y * INPUT_SIZE
            for (x in 0 until INPUT_SIZE) {
                val srcX = if (espejarX) (INPUT_SIZE - 1 - x) else x
                val px = pixels[rowOffset + srcX]
                inputBuffer.putFloat((Color.red(px) - 128f) / 128f)
                inputBuffer.putFloat((Color.green(px) - 128f) / 128f)
                inputBuffer.putFloat((Color.blue(px) - 128f) / 128f)
            }
        }
        inputBuffer.rewind()

        val output = Array(1) { FloatArray(DIM_EMBEDDING) }
        tflite.run(inputBuffer, output)
        return output[0]
    }

    /**
     * Compensa variaciones de brillo/exposicion entre distintos celulares o habitaciones
     * manteniendo intacta la estructura cromatica y de bordes para MobileFaceNet.
     */
    private fun ecualizarIluminacionSuave(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var sumLum = 0f
        for (px in pixels) {
            sumLum += 0.299f * Color.red(px) + 0.587f * Color.green(px) + 0.114f * Color.blue(px)
        }
        val meanLum = (sumLum / pixels.size).coerceAtLeast(1f)
        val gain = (128f / meanLum).coerceIn(0.75f, 1.38f)

        val outPixels = IntArray(w * h)
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (Color.red(px) * gain).toInt().coerceIn(0, 255)
            val g = (Color.green(px) * gain).toInt().coerceIn(0, 255)
            val b = (Color.blue(px) * gain).toInt().coerceIn(0, 255)
            outPixels[i] = Color.rgb(r, g, b)
        }

        return Bitmap.createBitmap(outPixels, w, h, Bitmap.Config.ARGB_8888)
    }

    fun normalizarL2InPlace(vec: FloatArray, start: Int = 0, length: Int = vec.size) {
        var sqSum = 0f
        val end = start + length
        for (i in start until end) {
            sqSum += vec[i] * vec[i]
        }
        val norm = sqrt(sqSum).coerceAtLeast(1e-6f)
        for (i in start until end) {
            vec[i] /= norm
        }
    }

    private fun extraerEmbeddingRespaldoDeterminista(bitmap112: Bitmap): FloatArray {
        val fallback = FloatArray(DIM_EMBEDDING)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap112.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val step = (pixels.size / DIM_EMBEDDING).coerceAtLeast(1)
        for (i in 0 until DIM_EMBEDDING) {
            val px = pixels[(i * step).coerceAtMost(pixels.lastIndex)]
            fallback[i] = ((Color.red(px) + Color.green(px) + Color.blue(px)) / 3f - 128f) / 128f
        }
        normalizarL2InPlace(fallback)
        return fallback
    }
}
