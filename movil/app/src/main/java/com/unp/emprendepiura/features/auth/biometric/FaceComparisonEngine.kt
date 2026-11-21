package com.unp.emprendepiura.features.auth.biometric

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Archivo: FaceComparisonEngine.kt
 * Proposito: Motor de verificacion biometrica 1:1 basado en similitud coseno y distancia
 *            euclidiana sobre embeddings neuronales de 192 dimensiones (MobileFaceNet)
 *            combinado con proporciones craneofaciales (24 dims).
 */
object FaceComparisonEngine {

    data class ResultadoComparacion(
        val coincide: Boolean,
        val porcentajeSimilitud: Int,
        val scoreEmbedding: Float = 0f,
        val distanciaEuclidiana: Float = 2f,
        val scoreGeometria: Float = 0f
    )

    private const val UMBRAL_COSENO_ESTRICTO = 0.68f
    private const val UMBRAL_DISTANCIA_MAX = 0.80f
    private const val UMBRAL_GEOMETRIA_MIN = 0.60f
    private const val UMBRAL_GLOBAL = 0.70f

    fun esDescriptorCompleto(vector: String): Boolean {
        return parseVector(vector).size == FaceAnalyzerUtil.TOTAL_DIM
    }

    fun compararRostros(vectorRegistrado: String, vectorActual: String): ResultadoComparacion {
        val v1 = parseVector(vectorRegistrado)
        val v2 = parseVector(vectorActual)

        if (v1.size != FaceAnalyzerUtil.TOTAL_DIM || v2.size != FaceAnalyzerUtil.TOTAL_DIM) {
            return ResultadoComparacion(coincide = false, porcentajeSimilitud = 0)
        }

        val dimEmb = FaceAnalyzerUtil.DIM_EMBEDDING
        val cosSim = similitudCosenoL2(v1, v2, 0, dimEmb)
        val distEuclidiana = sqrt((2f * (1f - cosSim)).coerceAtLeast(0f))
        val sGeo = similitudCraneofacial(v1, v2, dimEmb, FaceAnalyzerUtil.DIM_GEOMETRY)

        val scoreCombinado = (cosSim.coerceAtLeast(0f) * 0.82f + sGeo * 0.18f).coerceIn(0f, 1f)

        val coincide = (cosSim >= UMBRAL_COSENO_ESTRICTO) &&
            (distEuclidiana <= UMBRAL_DISTANCIA_MAX) &&
            (sGeo >= UMBRAL_GEOMETRIA_MIN) &&
            (scoreCombinado >= UMBRAL_GLOBAL)

        val porcentaje = if (coincide) {
            (85f + (cosSim - UMBRAL_COSENO_ESTRICTO).coerceAtLeast(0f) / (1f - UMBRAL_COSENO_ESTRICTO) * 14f)
                .roundToInt()
                .coerceIn(85, 99)
        } else {
            (cosSim.coerceAtLeast(0f) * 80f).roundToInt().coerceIn(0, 72)
        }

        return ResultadoComparacion(
            coincide = coincide,
            porcentajeSimilitud = porcentaje,
            scoreEmbedding = cosSim,
            distanciaEuclidiana = distEuclidiana,
            scoreGeometria = sGeo
        )
    }

    private fun similitudCosenoL2(
        a: FloatArray,
        b: FloatArray,
        start: Int,
        length: Int
    ): Float {
        val end = start + length
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in start until end) {
            val va = a[i]
            val vb = b[i]
            dot += va * vb
            normA += va * va
            normB += vb * vb
        }
        val denom = sqrt(normA * normB)
        return if (denom > 1e-6f) (dot / denom).coerceIn(-1f, 1f) else 0f
    }

    private fun similitudCraneofacial(
        a: FloatArray,
        b: FloatArray,
        start: Int,
        length: Int
    ): Float {
        val end = start + length
        var sumRelDiff = 0f
        for (i in start until end) {
            val va = a[i]
            val vb = b[i]
            val scale = max(max(abs(va), abs(vb)), 0.14f)
            sumRelDiff += abs(va - vb) / scale
        }
        val meanRelDiff = sumRelDiff / length.toFloat()
        return (1f - meanRelDiff * 3.2f).coerceIn(0f, 1f)
    }

    private fun parseVector(raw: String): FloatArray {
        if (raw.isBlank()) return FloatArray(0)
        return raw.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .toFloatArray()
    }
}
