package com.unp.emprendepiura.domain.validator

import com.unp.emprendepiura.domain.model.MetodoPago

/*
 * Archivo: MarketplaceValidator.kt
 * Proposito: Validaciones de negocio para emprendimientos, productos y checkout en Piura/UNP.
 */
object MarketplaceValidator {

    fun validarSolicitudEmprendimiento(
        nombreTienda: String,
        telefono: String,
        puntoEncuentro: String
    ): String? {
        if (nombreTienda.trim().length < 3) {
            return "Ingresa un nombre de emprendimiento de al menos 3 caracteres."
        }
        if (telefono.trim().length < 9 || !telefono.trim().all { it.isDigit() }) {
            return "Ingresa un celular de contacto válido (9 dígitos)."
        }
        if (puntoEncuentro.isBlank()) {
            return "Indica un punto de encuentro habitual en Piura o UNP."
        }
        return null
    }

    fun validarProducto(
        titulo: String,
        precioTexto: String,
        stockTexto: String,
        categoriaId: String
    ): String? {
        if (titulo.trim().length < 3) {
            return "Ingresa un título descriptivo para el producto."
        }
        val precio = precioTexto.replace(',', '.').toDoubleOrNull()
        if (precio == null || precio <= 0.0) {
            return "Ingresa un precio válido mayor a S/ 0.00."
        }
        val stock = stockTexto.toIntOrNull()
        if (stock == null || stock < 0) {
            return "Ingresa una cantidad de stock válida."
        }
        if (categoriaId.isBlank()) {
            return "Selecciona una categoría."
        }
        return null
    }

    fun validarCheckout(
        direccionOPunto: String,
        metodoPago: MetodoPago,
        tieneComprobante: Boolean
    ): String? {
        if (direccionOPunto.trim().length < 5) {
            return "Indica el punto de encuentro en UNP/Piura o tu dirección exacta."
        }
        if (metodoPago == MetodoPago.YAPE_PLIN && !tieneComprobante) {
            return "Adjunta la captura de pantalla del pago Yape/Plin para validar tu pedido."
        }
        return null
    }
}
