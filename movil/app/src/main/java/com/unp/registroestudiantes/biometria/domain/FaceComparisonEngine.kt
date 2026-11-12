package com.unp.registroestudiantes.biometria.domain

import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometrico
import com.unp.registroestudiantes.biometria.util.FaceAnalyzerUtil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Archivo: FaceComparisonEngine.kt
 * Proposito: Motor matematico de verificacion facial de triple compuerta (Correlacion de Pearson
 *            de media cero sobre HoG alineado + Contraste Z-Score + Proporciones Craneofaciales)
 *            para garantizar que solo el titular registrado sea admitido y rechazar a terceros.
 */
object FaceComparisonEngine {

    data class ResultadoComparacion(
        val coincide: Boolean,
        val porcentajeSimilitud: Int,
        val scoreHog: Float = 0f,
        val scoreApariencia: Float = 0f,
        val scoreGeometria: Float = 0f
    )

    data class ResultadoIdentificacion(
        val usuario: UsuarioBiometrico?,
        val comparacion: ResultadoComparacion
    )

    // Umbrales calibrados para alta discriminacion biometrica 1:1
    // Rechaza estrictamente a cualquier tercera persona mientras admite con fidelidad al titular genuino
    private const val UMBRAL_HOG = 0.60f
    private const val UMBRAL_GEOMETRIA = 0.64f
    private const val UMBRAL_APARIENCIA = 0.50f
    private const val UMBRAL_GLOBAL = 0.66f

    fun esDescriptorCompleto(vector: String): Boolean {
        return parseVector(vector).size == FaceAnalyzerUtil.TOTAL_DIM
    }

    /**
     * Compara dos descriptores faciales de 232 dimensiones exigiendo verificacion
     * simultanea en estructura HoG, proporciones anatomicas craneofaciales y apariencia tonal relativa.
     */
    fun compararRostros(vectorRegistrado: String, vectorActual: String): ResultadoComparacion {
        val v1 = parseVector(vectorRegistrado)
        val v2 = parseVector(vectorActual)

        if (v1.size != FaceAnalyzerUtil.TOTAL_DIM || v2.size != FaceAnalyzerUtil.TOTAL_DIM) {
            return ResultadoComparacion(coincide = false, porcentajeSimilitud = 0)
        }

        val endHog = FaceAnalyzerUtil.DIM_HOG
        val endApp = endHog + FaceAnalyzerUtil.DIM_APPEARANCE

        // 1. Estructura HoG con normalizacion de contraste (144 dims)
        val rHog = correlacionPearson(v1, v2, 0, FaceAnalyzerUtil.DIM_HOG)

        // 2. Apariencia tonal relativa a la media facial (64 dims)
        val rApp = correlacionPearson(v1, v2, endHog, FaceAnalyzerUtil.DIM_APPEARANCE)

        // 3. Proporciones anatomicas craneofaciales ML Kit (24 dims)
        val sGeo = similitudCraneofacial(v1, v2, endApp, FaceAnalyzerUtil.DIM_GEOMETRY)

        // Ponderacion equilibrada: 40% HoG, 35% Geometria facial, 25% Apariencia tonal
        val scoreCombinado = (rHog * 0.40f + sGeo * 0.35f + rApp * 0.25f).coerceIn(0f, 1f)

        // Verificacion 1:1 estricta: EXIGE que todas las compuertas pasen simultaneamente
        // para garantizar que solo el verdadero titular registrado sea admitido
        val coincide = (rHog >= UMBRAL_HOG) &&
                       (sGeo >= UMBRAL_GEOMETRIA) &&
                       (rApp >= UMBRAL_APARIENCIA) &&
                       (scoreCombinado >= UMBRAL_GLOBAL)

        // Escala visual de porcentaje para el usuario
        val porcentaje = if (coincide) {
            (84f + (scoreCombinado - UMBRAL_GLOBAL).coerceAtLeast(0f) / (1f - UMBRAL_GLOBAL) * 15f)
                .roundToInt()
                .coerceIn(85, 99)
        } else {
            (scoreCombinado * 74f).roundToInt().coerceIn(0, 72)
        }

        return ResultadoComparacion(
            coincide = coincide,
            porcentajeSimilitud = porcentaje,
            scoreHog = rHog,
            scoreApariencia = rApp,
            scoreGeometria = sGeo
        )
    }

    /**
     * Busca entre todos los usuarios registrados aquel cuyo descriptor facial supere las
     * 3 compuertas biometricas y tenga la mayor similitud con el rostro capturado en vivo.
     */
    fun identificarUsuario(
        usuarios: List<UsuarioBiometrico>,
        vectorActual: String
    ): ResultadoIdentificacion {
        var mejorUsuario: UsuarioBiometrico? = null
        var mejorResultado = ResultadoComparacion(coincide = false, porcentajeSimilitud = 0)
        var mejorScore = -1f

        for (u in usuarios) {
            val res = compararRostros(u.vectorFacial, vectorActual)
            val score = res.scoreHog * 0.45f + res.scoreApariencia * 0.35f + res.scoreGeometria * 0.20f
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

    private fun correlacionPearson(
        a: FloatArray,
        b: FloatArray,
        start: Int,
        length: Int
    ): Float {
        val end = start + length
        var meanA = 0f
        var meanB = 0f
        for (i in start until end) {
            meanA += a[i]
            meanB += b[i]
        }
        meanA /= length.toFloat()
        meanB /= length.toFloat()

        var num = 0f
        var denA = 0f
        var denB = 0f
        for (i in start until end) {
            val da = a[i] - meanA
            val db = b[i] - meanB
            num += da * db
            denA += da * da
            denB += db * db
        }

        val denom = sqrt(denA * denB)
        return if (denom > 1e-6f) (num / denom).coerceIn(-1f, 1f) else 0f
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
            val scale = max(max(abs(va), abs(vb)), 0.20f)
            sumRelDiff += abs(va - vb) / scale
        }
        val meanRelDiff = sumRelDiff / length.toFloat()
        return (1f - meanRelDiff * 3.0f).coerceIn(0f, 1f)
    }

    private fun parseVector(raw: String): FloatArray {
        if (raw.isBlank()) return FloatArray(0)
        return raw.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .toFloatArray()
    }
}
