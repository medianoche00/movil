package com.unp.registroestudiantes.biometria.domain

import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometrico
import com.unp.registroestudiantes.biometria.util.FaceAnalyzerUtil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Archivo: FaceComparisonEngine.kt
 * Proposito: Motor de verificacion biometrica 1:1 basado en similitud coseno y distancia
 *            euclidiana sobre embeddings neuronales de 192 dimensiones (MobileFaceNet TFLite)
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

    data class ResultadoIdentificacion(
        val usuario: UsuarioBiometrico?,
        val comparacion: ResultadoComparacion
    )

    // Umbrales calibrados para MobileFaceNet (192-D L2-normalizado con Test-Time Augmentation):
    // - Dos personas distintas obtienen cosSim en [-0.10, 0.50] (distancia > 1.00)
    // - La misma persona (incluso en otro celular o iluminacion) obtiene cosSim en [0.70, 0.96]
    private const val UMBRAL_COSENO_ESTRICTO = 0.68f
    private const val UMBRAL_DISTANCIA_MAX = 0.80f
    private const val UMBRAL_GEOMETRIA_MIN = 0.60f
    private const val UMBRAL_GLOBAL = 0.70f

    fun esDescriptorCompleto(vector: String): Boolean {
        return parseVector(vector).size == FaceAnalyzerUtil.TOTAL_DIM
    }

    /**
     * Compara dos descriptores faciales hibridos de 216 dimensiones (192 MobileFaceNet + 24 Geometria),
     * exigiendo coincidencia simultanea en el espacio neuronal de identidad y en geometria facial.
     */
    fun compararRostros(vectorRegistrado: String, vectorActual: String): ResultadoComparacion {
        val v1 = parseVector(vectorRegistrado)
        val v2 = parseVector(vectorActual)

        if (v1.size != FaceAnalyzerUtil.TOTAL_DIM || v2.size != FaceAnalyzerUtil.TOTAL_DIM) {
            return ResultadoComparacion(coincide = false, porcentajeSimilitud = 0)
        }

        val dimEmb = FaceAnalyzerUtil.DIM_EMBEDDING

        // 1. Similitud coseno y distancia euclidiana sobre la hiperesfera L2 de MobileFaceNet (192 dims)
        val cosSim = similitudCosenoL2(v1, v2, 0, dimEmb)
        val distEuclidiana = sqrt((2f * (1f - cosSim)).coerceAtLeast(0f))

        // 2. Proporciones anatomicas craneofaciales (24 dims)
        val sGeo = similitudCraneofacial(v1, v2, dimEmb, FaceAnalyzerUtil.DIM_GEOMETRY)

        // Ponderacion centrada en el embedding neuronal de MobileFaceNet (82%) + Geometria (18%)
        val scoreCombinado = (cosSim.coerceAtLeast(0f) * 0.82f + sGeo * 0.18f).coerceIn(0f, 1f)

        // Compuerta estricta 1:1: rechaza a cualquier tercera persona y admite al titular genuino
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

    fun identificarUsuario(
        usuarios: List<UsuarioBiometrico>,
        vectorActual: String
    ): ResultadoIdentificacion {
        var mejorUsuario: UsuarioBiometrico? = null
        var mejorResultado = ResultadoComparacion(coincide = false, porcentajeSimilitud = 0)
        var mejorScore = -1f

        for (u in usuarios) {
            val res = compararRostros(u.vectorFacial, vectorActual)
            val score = res.scoreEmbedding * 0.82f + res.scoreGeometria * 0.18f
            if (res.coincide && score > mejorScore) {
                mejorScore = score
                mejorUsuario = u
                mejorResultado = res
            } else if (mejorUsuario == null && score > mejorScore) {
                mejorScore = score
                mejorResultado = res
            }
        }

        return ResultadoIdentificacion(
            usuario = mejorUsuario,
            comparacion = mejorResultado
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
