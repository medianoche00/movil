package com.unp.registroestudiantes.biometria.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Archivo: FaceAnalyzerUtil.kt
 * Proposito: Analizador facial en tiempo real con Google ML Kit y extraccion de descriptor
 *            biometrico de 232 dimensiones (HoG alineado por ojos + contraste Z-score +
 *            proporciones craneofaciales) para distinguir con precision a cada persona.
 */
object FaceAnalyzerUtil {

    const val DIM_HOG = 144
    const val DIM_APPEARANCE = 64
    const val DIM_GEOMETRY = 24
    const val TOTAL_DIM = DIM_HOG + DIM_APPEARANCE + DIM_GEOMETRY

    sealed class EstadoFrameFacial {
        data class SinRostro(val mensaje: String) : EstadoFrameFacial()
        data class Desalineado(val mensaje: String) : EstadoFrameFacial()
        data class RostroCentrado(
            val vectorFacial: String,
            val frameBitmap: Bitmap
        ) : EstadoFrameFacial()
    }

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.24f)
        .build()

    /**
     * Analiza un frame en vivo proveniente de CameraX PreviewView, verificando centrado,
     * distancia, postura frontal, ojos abiertos y extrayendo el descriptor biometrico.
     */
    suspend fun analizarFrameEnVivo(bitmap: Bitmap): EstadoFrameFacial {
        if (bitmap.width < 64 || bitmap.height < 64) {
            return EstadoFrameFacial.SinRostro("Inicializando cámara frontal...")
        }

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val detector = FaceDetection.getClient(detectorOptions)

        val facesResult: Result<List<Face>> = suspendCancellableCoroutine { cont ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    detector.close()
                    if (cont.isActive) cont.resume(Result.success(faces))
                }
                .addOnFailureListener { err ->
                    detector.close()
                    if (cont.isActive) cont.resume(Result.failure(err))
                }
        }

        val faces = facesResult.getOrElse {
            return EstadoFrameFacial.SinRostro("Ubica tu rostro en el centro del recuadro...")
        }

        if (faces.isEmpty()) {
            return EstadoFrameFacial.SinRostro("Ubica tu rostro en el centro del recuadro...")
        }

        if (faces.size > 1) {
            return EstadoFrameFacial.Desalineado("Solo debe aparecer una persona frente a la cámara")
        }

        val face = faces.first()
        val box = face.boundingBox
        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        val relW = box.width().toFloat() / imgW
        val relH = box.height().toFloat() / imgH
        if (relW < 0.24f || relH < 0.20f) {
            return EstadoFrameFacial.Desalineado("Acércate un poco más a la cámara")
        }
        if (relW > 0.88f || relH > 0.88f) {
            return EstadoFrameFacial.Desalineado("Aléjate ligeramente de la cámara")
        }

        val centerX = box.exactCenterX() / imgW
        val centerY = box.exactCenterY() / imgH
        if (abs(centerX - 0.5f) > 0.20f || abs(centerY - 0.5f) > 0.22f) {
            return EstadoFrameFacial.Desalineado("Centra tu rostro dentro del recuadro guía")
        }

        if (abs(face.headEulerAngleY) > 18f || abs(face.headEulerAngleZ) > 16f || abs(face.headEulerAngleX) > 18f) {
            return EstadoFrameFacial.Desalineado("Mira directamente de frente sin inclinar la cabeza")
        }

        val leftOpen = face.leftEyeOpenProbability ?: 0.9f
        val rightOpen = face.rightEyeOpenProbability ?: 0.9f
        if (leftOpen < 0.22f || rightOpen < 0.22f) {
            return EstadoFrameFacial.Desalineado("Mantén ambos ojos abiertos mirando a la cámara")
        }

        val rawEye1 = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rawEye2 = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouth1 = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouth2 = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

        if (rawEye1 == null || rawEye2 == null || nose == null || mouth1 == null || mouth2 == null) {
            return EstadoFrameFacial.Desalineado("Asegura buena iluminación en tu rostro")
        }

        val vector = extraerDescriptorCompleto(bitmap, face, rawEye1, rawEye2)
        return EstadoFrameFacial.RostroCentrado(
            vectorFacial = vector,
            frameBitmap = bitmap
        )
    }

    /**
     * Promedia multiples vectores capturados en frames consecutivos para eliminar ruido
     * de iluminacion o micro-movimientos y obtener una huella facial de alta fidelidad.
     */
    fun promediarVectores(vectores: List<String>): String {
        if (vectores.isEmpty()) return ""
        val parsed = vectores.map { raw ->
            raw.split(",").mapNotNull { it.trim().toFloatOrNull() }.toFloatArray()
        }.filter { it.size == TOTAL_DIM }

        if (parsed.isEmpty()) return vectores.last()

        val avg = FloatArray(TOTAL_DIM)
        val count = parsed.size.toFloat()
        for (vec in parsed) {
            for (i in 0 until TOTAL_DIM) {
                avg[i] += vec[i] / count
            }
        }

        // Re-estandarizar los bloques Z-score tras el promedio
        estandarizarZScoreInPlace(avg, 0, DIM_HOG)
        estandarizarZScoreInPlace(avg, DIM_HOG, DIM_APPEARANCE)

        return avg.joinToString(",") { String.format(Locale.US, "%.4f", it) }
    }

    fun comprimirBitmapJpeg(bitmap: Bitmap, quality: Int = 88): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    fun guardarBitmapTemporal(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "rostros_temp").apply { mkdirs() }
        val file = File(dir, "rostro_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        return Uri.fromFile(file)
    }

    /**
     * Extrae el descriptor de 232 dimensiones a partir de un Bitmap almacenado previamente
     * para migrar automaticamente registros creados con versiones anteriores del escaner.
     */
    suspend fun extraerVectorDesdeBitmap(bitmap: Bitmap): String? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val detector = FaceDetection.getClient(detectorOptions)
        val faces: List<Face> = suspendCancellableCoroutine { cont ->
            detector.process(inputImage)
                .addOnSuccessListener { list ->
                    detector.close()
                    if (cont.isActive) cont.resume(list)
                }
                .addOnFailureListener {
                    detector.close()
                    if (cont.isActive) cont.resume(emptyList())
                }
        }
        val face = faces.firstOrNull() ?: return null
        val rawEye1 = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return null
        val rawEye2 = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return null
        return extraerDescriptorCompleto(bitmap, face, rawEye1, rawEye2)
    }

    private fun extraerDescriptorCompleto(
        sourceBitmap: Bitmap,
        face: Face,
        eyeA: PointF,
        eyeB: PointF
    ): String {
        // Ordenar ojos de izquierda a derecha en coordenadas de imagen para invariancia de espejo
        val leftEye = if (eyeA.x <= eyeB.x) eyeA else eyeB
        val rightEye = if (eyeA.x <= eyeB.x) eyeB else eyeA

        val aligned96 = alinearRostroPorOjos(sourceBitmap, leftEye, rightEye)
        val hogBlock = extraerBloqueHoG(aligned96)
        val appearanceBlock = extraerBloqueApariencia(aligned96)
        val geometryBlock = extraerBloqueGeometrico(face, leftEye, rightEye)

        val descriptor = FloatArray(TOTAL_DIM)
        System.arraycopy(hogBlock, 0, descriptor, 0, DIM_HOG)
        System.arraycopy(appearanceBlock, 0, descriptor, DIM_HOG, DIM_APPEARANCE)
        System.arraycopy(geometryBlock, 0, descriptor, DIM_HOG + DIM_APPEARANCE, DIM_GEOMETRY)

        return descriptor.joinToString(",") { String.format(Locale.US, "%.4f", it) }
    }

    /**
     * Transforma afínmente el rostro para que el ojo izquierdo quede en (30, 34)
     * y el ojo derecho en (66, 34) dentro de un lienzo canonico de 96x96 pixeles.
     */
    private fun alinearRostroPorOjos(
        source: Bitmap,
        leftEye: PointF,
        rightEye: PointF
    ): Bitmap {
        val targetSize = 96
        val targetLeftX = 30f
        val targetRightX = 66f
        val targetEyeY = 34f
        val targetDist = targetRightX - targetLeftX // 36 px

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

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Extrae 144 caracteristicas de gradientes orientados (6x6 celdas x 4 orientaciones)
     * sobre el rostro alineado de 96x96 con normalizacion previa de luminancia para invarianza a la luz.
     */
    private fun extraerBloqueHoG(aligned96: Bitmap): FloatArray {
        val size = 96
        val pixels = IntArray(size * size)
        aligned96.getPixels(pixels, 0, size, 0, 0, size, size)

        val lum = FloatArray(size * size)
        var sumLum = 0f
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            lum[i] = l
            sumLum += l
        }

        // Normalizacion global de iluminacion: elimina el sesgo entre habitaciones oscuras/claras
        val meanLum = sumLum / (size * size)
        var varLum = 0f
        for (i in pixels.indices) {
            val diff = lum[i] - meanLum
            varLum += diff * diff
        }
        val stdLum = sqrt(varLum / (size * size)).coerceAtLeast(0.08f)
        for (i in pixels.indices) {
            lum[i] = (lum[i] - meanLum) / stdLum
        }

        val gridCells = 6
        val cellSpan = size / gridCells // 16 px
        val hog = FloatArray(DIM_HOG)
        var idx = 0

        for (cy in 0 until gridCells) {
            for (cx in 0 until gridCells) {
                var sumH = 0f
                var sumV = 0f
                var sumD1 = 0f
                var sumD2 = 0f
                var count = 0

                val startY = (cy * cellSpan).coerceAtLeast(1)
                val endY = ((cy + 1) * cellSpan).coerceAtMost(size - 1)
                val startX = (cx * cellSpan).coerceAtLeast(1)
                val endX = ((cx + 1) * cellSpan).coerceAtMost(size - 1)

                for (y in startY until endY) {
                    val row = y * size
                    for (x in startX until endX) {
                        val gx = lum[row + x + 1] - lum[row + x - 1]
                        val gy = lum[(y + 1) * size + x] - lum[(y - 1) * size + x]
                        val gd1 = (lum[(y + 1) * size + x + 1] - lum[(y - 1) * size + x - 1]) * 0.7071f
                        val gd2 = (lum[(y + 1) * size + x - 1] - lum[(y - 1) * size + x + 1]) * 0.7071f

                        sumH += abs(gx)
                        sumV += abs(gy)
                        sumD1 += abs(gd1)
                        sumD2 += abs(gd2)
                        count++
                    }
                }

                val inv = if (count > 0) 1f / count else 1f
                hog[idx++] = sumH * inv
                hog[idx++] = sumV * inv
                hog[idx++] = sumD1 * inv
                hog[idx++] = sumD2 * inv
            }
        }

        estandarizarZScoreInPlace(hog, 0, DIM_HOG)
        return hog
    }

    /**
     * Extrae 64 caracteristicas de contraste tonal relativo (malla 8x8 de la zona interna del rostro),
     * restando la luminancia media facial para que no dependa de la intensidad o fuente de luz.
     */
    private fun extraerBloqueApariencia(aligned96: Bitmap): FloatArray {
        val size = 96
        val grid = 8
        val start = 8
        val span = 10 // 8 + 8*10 = 88 (cubre la region interna 8..88 de 96)
        val appearance = FloatArray(DIM_APPEARANCE)
        var idx = 0

        val pixels = IntArray(size * size)
        aligned96.getPixels(pixels, 0, size, 0, 0, size, size)

        var faceMeanLum = 0f
        var faceCount = 0
        for (y in 8 until 88) {
            val row = y * size
            for (x in 8 until 88) {
                val c = pixels[row + x]
                val luma = (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f
                faceMeanLum += luma
                faceCount++
            }
        }
        faceMeanLum = if (faceCount > 0) faceMeanLum / faceCount else 0.5f

        for (gy in 0 until grid) {
            for (gx in 0 until grid) {
                var sumTone = 0f
                var count = 0
                val y0 = start + gy * span
                val x0 = start + gx * span
                for (y in y0 until (y0 + span)) {
                    val row = y * size
                    for (x in x0 until (x0 + span)) {
                        val c = pixels[row + x]
                        val r = Color.red(c) / 255f
                        val g = Color.green(c) / 255f
                        val b = Color.blue(c) / 255f
                        val luma = 0.299f * r + 0.587f * g + 0.114f * b
                        val rgDiff = (r - g) * 0.25f
                        sumTone += (luma - faceMeanLum) + rgDiff
                        count++
                    }
                }
                appearance[idx++] = if (count > 0) sumTone / count else 0f
            }
        }

        estandarizarZScoreInPlace(appearance, 0, DIM_APPEARANCE)
        return appearance
    }

    /**
     * Extrae 24 proporciones craneofaciales normalizadas respecto a la distancia interocular
     * en el sistema de coordenadas rotado por el eje de los ojos.
     */
    private fun extraerBloqueGeometrico(
        face: Face,
        leftEye: PointF,
        rightEye: PointF
    ): FloatArray {
        val dx = rightEye.x - leftEye.x
        val dy = rightEye.y - leftEye.y
        val eyeDist = hypot(dx, dy).coerceAtLeast(10f)
        val angle = -atan2(dy, dx)
        val cosA = cos(angle)
        val sinA = sin(angle)
        val eyeMidX = (leftEye.x + rightEye.x) * 0.5f
        val eyeMidY = (leftEye.y + rightEye.y) * 0.5f

        // Proyecta cualquier punto al sistema canonico centrado en el entrecejo y escalado por eyeDist
        fun project(pt: PointF): PointF {
            val rx = pt.x - eyeMidX
            val ry = pt.y - eyeMidY
            val ux = (rx * cosA - ry * sinA) / eyeDist
            val uy = (rx * sinA + ry * cosA) / eyeDist
            return PointF(ux, uy)
        }

        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position?.let(::project) ?: PointF(0f, 0.58f)
        val mA = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.let(::project) ?: PointF(-0.42f, 1.05f)
        val mB = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.let(::project) ?: PointF(0.42f, 1.05f)
        val mLeft = if (mA.x <= mB.x) mA else mB
        val mRight = if (mA.x <= mB.x) mB else mA
        val mBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position?.let(::project) ?: PointF(0f, 1.22f)

        val cA = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.let(::project) ?: PointF(-0.65f, 0.55f)
        val cB = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.let(::project) ?: PointF(0.65f, 0.55f)
        val cLeft = if (cA.x <= cB.x) cA else cB
        val cRight = if (cA.x <= cB.x) cB else cA

        val mouthWidth = hypot(mRight.x - mLeft.x, mRight.y - mLeft.y).coerceIn(0.4f, 1.6f)
        val mouthMidY = (mLeft.y + mRight.y) * 0.5f
        val noseY = nose.y.coerceIn(0.25f, 1.2f)
        val noseX = nose.x.coerceIn(-0.4f, 0.4f)
        val mouthToNoseY = (mouthMidY - noseY).coerceIn(0.15f, 1.0f)
        val mouthBottomY = mBottom.y.coerceIn(0.6f, 1.8f)
        val lowerLipDrop = (mouthBottomY - mouthMidY).coerceIn(0.02f, 0.6f)
        val cheekSpan = hypot(cRight.x - cLeft.x, cRight.y - cLeft.y).coerceIn(0.7f, 2.2f)
        val cheekMidY = ((cLeft.y + cRight.y) * 0.5f).coerceIn(0.2f, 1.2f)
        val noseToMouthRatio = (noseY / mouthWidth.coerceAtLeast(0.3f)).coerceIn(0.3f, 2.0f)

        val boxW = (face.boundingBox.width() / eyeDist).coerceIn(1.4f, 3.2f)
        val boxH = (face.boundingBox.height() / eyeDist).coerceIn(1.6f, 3.8f)
        val boxAspect = (boxW / boxH).coerceIn(0.5f, 1.3f)

        // Contornos detallados de ML Kit si estan presentes en el frame
        fun contourSpanX(type: Int, fallback: Float): Float {
            val pts = face.getContour(type)?.points?.map(::project) ?: return fallback
            if (pts.size < 2) return fallback
            val minX = pts.minOf { it.x }
            val maxX = pts.maxOf { it.x }
            return (maxX - minX).coerceIn(0.1f, 2.5f)
        }

        fun contourSpanY(type: Int, fallback: Float): Float {
            val pts = face.getContour(type)?.points?.map(::project) ?: return fallback
            if (pts.size < 2) return fallback
            val minY = pts.minOf { it.y }
            val maxY = pts.maxOf { it.y }
            return (maxY - minY).coerceIn(0.02f, 2.5f)
        }

        fun contourMeanY(type: Int, fallback: Float): Float {
            val pts = face.getContour(type)?.points?.map(::project) ?: return fallback
            if (pts.isEmpty()) return fallback
            return (pts.sumOf { it.y.toDouble() }.toFloat() / pts.size).coerceIn(-1.5f, 2.5f)
        }

        val eye1W = contourSpanX(FaceContour.LEFT_EYE, 0.42f)
        val eye2W = contourSpanX(FaceContour.RIGHT_EYE, 0.42f)
        val eye1H = contourSpanY(FaceContour.LEFT_EYE, 0.14f)
        val eye2H = contourSpanY(FaceContour.RIGHT_EYE, 0.14f)
        val brow1Y = abs(contourMeanY(FaceContour.LEFT_EYEBROW_TOP, -0.26f)).coerceIn(0.08f, 0.7f)
        val brow2Y = abs(contourMeanY(FaceContour.RIGHT_EYEBROW_TOP, -0.26f)).coerceIn(0.08f, 0.7f)
        val noseBottomW = contourSpanX(FaceContour.NOSE_BOTTOM, 0.52f)
        val noseBridgeH = contourSpanY(FaceContour.NOSE_BRIDGE, 0.50f)
        val upperLipY = contourMeanY(FaceContour.UPPER_LIP_TOP, 0.92f).coerceIn(0.4f, 1.6f)
        val lowerLipY = contourMeanY(FaceContour.LOWER_LIP_BOTTOM, 1.20f).coerceIn(0.5f, 1.9f)
        val faceOvalW = contourSpanX(FaceContour.FACE, 2.15f)
        val faceOvalH = contourSpanY(FaceContour.FACE, 2.65f)

        return floatArrayOf(
            mouthWidth,
            mouthMidY,
            noseY,
            noseX,
            mouthToNoseY,
            mouthBottomY,
            lowerLipDrop,
            cheekSpan,
            cheekMidY,
            noseToMouthRatio,
            boxW,
            boxH,
            boxAspect,
            eye1W,
            eye2W,
            eye1H,
            eye2H,
            brow1Y,
            brow2Y,
            noseBottomW,
            noseBridgeH,
            upperLipY,
            lowerLipY,
            faceOvalW / faceOvalH.coerceAtLeast(0.8f)
        )
    }

    private fun estandarizarZScoreInPlace(arr: FloatArray, start: Int, length: Int) {
        if (length <= 0) return
        var sum = 0f
        val end = start + length
        for (i in start until end) {
            sum += arr[i]
        }
        val mean = sum / length.toFloat()

        var sqSum = 0f
        for (i in start until end) {
            val d = arr[i] - mean
            arr[i] = d
            sqSum += d * d
        }
        val std = sqrt(sqSum / length.toFloat()).coerceAtLeast(1e-4f)
        for (i in start until end) {
            arr[i] = (arr[i] / std).coerceIn(-3.5f, 3.5f)
        }
    }
}
