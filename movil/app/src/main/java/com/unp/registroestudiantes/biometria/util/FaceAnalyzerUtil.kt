package com.unp.registroestudiantes.biometria.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
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

/*
 * Archivo: FaceAnalyzerUtil.kt
 * Proposito: Validador de postura facial en vivo con Google ML Kit y orquestador del descriptor
 *            hibrido de 216 dimensiones (192 dims MobileFaceNet TFLite + 24 dims craneofaciales).
 */
object FaceAnalyzerUtil {

    const val DIM_EMBEDDING = MobileFaceNetEmbedder.DIM_EMBEDDING // 192
    const val DIM_GEOMETRY = CraniofacialFeatureExtractor.DIM_GEOMETRY // 24
    const val TOTAL_DIM = DIM_EMBEDDING + DIM_GEOMETRY // 216

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
     * distancia, postura frontal, ojos abiertos y extrayendo el embedding MobileFaceNet + geometria.
     */
    suspend fun analizarFrameEnVivo(context: Context, bitmap: Bitmap): EstadoFrameFacial {
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

        if (abs(face.headEulerAngleY) > 16f || abs(face.headEulerAngleZ) > 15f || abs(face.headEulerAngleX) > 16f) {
            return EstadoFrameFacial.Desalineado("Mira directamente de frente sin inclinar la cabeza")
        }

        val leftOpen = face.leftEyeOpenProbability ?: 0.9f
        val rightOpen = face.rightEyeOpenProbability ?: 0.9f
        if (leftOpen < 0.25f || rightOpen < 0.25f) {
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

        val vector = extraerDescriptorHibrido(context, bitmap, face, rawEye1, rawEye2)
        return EstadoFrameFacial.RostroCentrado(
            vectorFacial = vector,
            frameBitmap = bitmap
        )
    }

    /**
     * Promedia los vectores capturados en frames estables consecutivos y re-normaliza L2
     * el bloque de 192 dimensiones de MobileFaceNet sobre la hiperesfera unitaria.
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

        MobileFaceNetEmbedder.normalizarL2InPlace(avg, start = 0, length = DIM_EMBEDDING)
        return avg.joinToString(",") { String.format(Locale.US, "%.5f", it) }
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
     * Extrae el descriptor de 216 dimensiones (MobileFaceNet + geometria) desde un Bitmap
     * almacenado para actualizar automaticamente registros previos.
     */
    suspend fun extraerVectorDesdeBitmap(context: Context, bitmap: Bitmap): String? {
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
        return extraerDescriptorHibrido(context, bitmap, face, rawEye1, rawEye2)
    }

    private fun extraerDescriptorHibrido(
        context: Context,
        sourceBitmap: Bitmap,
        face: Face,
        eyeA: PointF,
        eyeB: PointF
    ): String {
        val leftEye = if (eyeA.x <= eyeB.x) eyeA else eyeB
        val rightEye = if (eyeA.x <= eyeB.x) eyeB else eyeA

        val embeddingBlock = MobileFaceNetEmbedder.extraerEmbedding(context, sourceBitmap, leftEye, rightEye)
        val geometryBlock = CraniofacialFeatureExtractor.extraerBloqueGeometrico(face, leftEye, rightEye)

        val descriptor = FloatArray(TOTAL_DIM)
        System.arraycopy(embeddingBlock, 0, descriptor, 0, DIM_EMBEDDING)
        System.arraycopy(geometryBlock, 0, descriptor, DIM_EMBEDDING, DIM_GEOMETRY)

        return descriptor.joinToString(",") { String.format(Locale.US, "%.5f", it) }
    }
}
