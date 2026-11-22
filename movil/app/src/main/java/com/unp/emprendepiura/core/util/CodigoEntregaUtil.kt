package com.unp.emprendepiura.core.util

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/*
 * Archivo: CodigoEntregaUtil.kt
 * Proposito: Generacion y validacion TOTP (HMAC-SHA256) de codigos PIN dinamicos de 6 digitos
 *            con rotacion automatica cada 90 segundos para entregas seguras.
 */
object CodigoEntregaUtil {

    const val PERIODO_SEGUNDOS: Long = 90L
    private const val CLAVE_APP_PIN = "EMPRENDEPIURA_UNP_DELIVERY_TOTP_V2_2026"

    fun ventanaActual(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Long =
        nowEpochSeconds / PERIODO_SEGUNDOS

    fun segundosRestantes(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Int {
        val transcurridos = nowEpochSeconds % PERIODO_SEGUNDOS
        val restantes = PERIODO_SEGUNDOS - transcurridos
        return restantes.toInt().coerceIn(1, PERIODO_SEGUNDOS.toInt())
    }

    /**
     * Genera un PIN dinamico de 6 digitos determinista para la orden y ventana de 90 segundos.
     * Ejemplo: "482917".
     */
    fun obtenerCodigoDinamico(
        ordenId: String,
        codigoOrden: String,
        ventana: Long = ventanaActual()
    ): String {
        val secreto = "$CLAVE_APP_PIN|${ordenId.trim()}|${codigoOrden.trim()}"
            .toByteArray(Charsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secreto, "HmacSHA256"))

        val msg = ByteBuffer.allocate(8).putLong(ventana).array()
        val hash = mac.doFinal(msg)

        // Truncado dinamico estandar RFC 4226 / 6238
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binario =
            ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val codigo = binario % 1_000_000
        return codigo.toString().padStart(6, '0')
    }

    /**
     * Valida el codigo de 6 digitos aceptando la ventana vigente (T) y tolerancia +/-1 ventana
     * para evitar falsos rechazos si el codigo rota justo al momento de confirmarlo.
     */
    fun validarCodigoEntrega(
        ordenId: String,
        codigoOrden: String,
        pinIngresado: String,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): Boolean {
        val limpio = pinIngresado.filter { it.isDigit() }
        if (limpio.length != 6) return false

        val ventana = ventanaActual(nowEpochSeconds)
        return (-1L..1L).any { delta ->
            obtenerCodigoDinamico(ordenId, codigoOrden, ventana + delta) == limpio
        }
    }

    /**
     * Formatea los 6 digitos en dos bloques de 3 para lectura rapida ("482  917").
     */
    fun formatearPin6(pin: String): String {
        val limpio = pin.filter { it.isDigit() }.padStart(6, '0').take(6)
        return "${limpio.substring(0, 3)}  ${limpio.substring(3, 6)}"
    }
}
