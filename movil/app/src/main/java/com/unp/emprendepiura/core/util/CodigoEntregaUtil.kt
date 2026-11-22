package com.unp.emprendepiura.core.util

import kotlin.math.abs

/*
 * Archivo: CodigoEntregaUtil.kt
 * Proposito: Generacion determinista y validacion antifraude de codigos PIN de confirmacion
 *            de entrega (estilo Uber Eats / Rappi) para comprador y emprendedor local.
 */
object CodigoEntregaUtil {

    /**
     * Genera un PIN numerico de seguridad de 4 digitos único y determinista para cada pedido.
     * Ejemplo: "4829", "7193".
     */
    fun obtenerCodigoEntrega(ordenId: String, codigoOrden: String): String {
        val semilla = (ordenId.trim() + codigoOrden.trim()).ifBlank { "unp_delivery_safe" }
        val hash = abs(semilla.hashCode())
        val pin = (hash % 9000) + 1000
        return pin.toString()
    }

    /**
     * Valida si el codigo ingresado por el emprendedor o repartidor coincide con el PIN del pedido.
     */
    fun validarCodigoEntrega(ordenId: String, codigoOrden: String, pinIngresado: String): Boolean {
        val esperado = obtenerCodigoEntrega(ordenId, codigoOrden)
        val limpio = pinIngresado.replace("\\s".toRegex(), "").replace("-", "").trim()
        return limpio == esperado
    }

    /**
     * Retorna el codigo formateado con espacios para maxima legibilidad visual (ej: "4  8  2  9").
     */
    fun formatearPinEspaciado(pin: String): String {
        return pin.map { "$it " }.joinToString("").trim()
    }
}
