package com.unp.emprendepiura.domain.model

/*
 * Archivo: ChatModels.kt
 * Proposito: Modelos de dominio para conversaciones y mensajeria en tiempo real y metricas Admin.
 */
data class Conversacion(
    val id: String,
    val compradorId: String,
    val emprendimientoId: String,
    val productoId: String? = null,
    val ordenId: String? = null,
    val ultimoMensaje: String = "",
    val actualizadoEn: String = "",
    val nombreContraparte: String = "",
    val subtituloProducto: String = ""
)

data class Mensaje(
    val id: String = "",
    val conversacionId: String,
    val remitenteId: String,
    val contenido: String,
    val leido: Boolean = false,
    val creadoEn: String = ""
)

data class AdminDashboardStats(
    val totalUsuarios: Int = 0,
    val emprendimientosAprobados: Int = 0,
    val emprendimientosPendientes: Int = 0,
    val productosActivos: Int = 0,
    val totalOrdenes: Int = 0,
    val montoTotalVentas: Double = 0.0
)
