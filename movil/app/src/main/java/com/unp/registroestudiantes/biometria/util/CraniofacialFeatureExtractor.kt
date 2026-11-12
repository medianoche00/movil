package com.unp.registroestudiantes.biometria.util

import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * Archivo: CraniofacialFeatureExtractor.kt
 * Proposito: Extractor modular de las 24 proporciones anatomicas craneofaciales normalizadas
 *            respecto a la distancia interocular, usado como segunda compuerta de verificacion
 *            junto al embedding neuronal de MobileFaceNet.
 */
object CraniofacialFeatureExtractor {

    const val DIM_GEOMETRY = 24

    fun extraerBloqueGeometrico(
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

        fun contourSpanX(type: Int, fallback: Float): Float {
            val pts = face.getContour(type)?.points?.map(::project) ?: return fallback
            if (pts.size < 2) return fallback
            return (pts.maxOf { it.x } - pts.minOf { it.x }).coerceIn(0.1f, 2.5f)
        }

        fun contourSpanY(type: Int, fallback: Float): Float {
            val pts = face.getContour(type)?.points?.map(::project) ?: return fallback
            if (pts.size < 2) return fallback
            return (pts.maxOf { it.y } - pts.minOf { it.y }).coerceIn(0.02f, 2.5f)
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
}
