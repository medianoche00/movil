package com.unp.emprendepiura.domain.model

/*
 * Archivo: ChatModels.kt
 * Proposito: Modelos de dominio para conversaciones, mensajeria en tiempo real,
 *            eventos Realtime (presencia, escritura, lectura, bloqueos) y metricas Admin.
 */
data class Conversacion(
    val id: String,
    val compradorId: String,
    val emprendimientoId: String,
    val productoId: String? = null,
    val ordenId: String? = null,
    val ultimoMensaje: String = "",
    val actualizadoEn: String = "",
    val bloqueadoPor: String? = null,
    val bloqueadoEn: String? = null,
    val mensajesNoLeidos: Int = 0,
    val nombreContraparte: String = "",
    val subtituloProducto: String = ""
) {
    val estaBloqueada: Boolean
        get() = !bloqueadoPor.isNullOrBlank()

    fun bloqueadaPorMi(miUsuarioId: String): Boolean =
        miUsuarioId.isNotBlank() && bloqueadoPor == miUsuarioId
}

data class Mensaje(
    val id: String = "",
    val conversacionId: String,
    val remitenteId: String,
    val contenido: String,
    val leido: Boolean = false,
    val creadoEn: String = ""
)

sealed interface ChatRoomRealtimeEvent {
    data class NuevoMensaje(val mensaje: Mensaje) : ChatRoomRealtimeEvent
    data class MensajeActualizado(val mensaje: Mensaje) : ChatRoomRealtimeEvent
    data class ConversacionActualizada(val conversacion: Conversacion) : ChatRoomRealtimeEvent
    data class ConversacionEliminada(val conversacionId: String) : ChatRoomRealtimeEvent
    data class ContraparteEscribiendo(val escribiendo: Boolean) : ChatRoomRealtimeEvent
    data class ContraparteEnLinea(val enLinea: Boolean) : ChatRoomRealtimeEvent
    data object Reconectado : ChatRoomRealtimeEvent
}

sealed interface ChatInboxRealtimeEvent {
    data class MensajeRecibido(val mensaje: Mensaje) : ChatInboxRealtimeEvent
    data object ConversacionesActualizadas : ChatInboxRealtimeEvent
    data object Reconectado : ChatInboxRealtimeEvent
}

data class AdminDashboardStats(
    val totalUsuarios: Int = 0,
    val emprendimientosAprobados: Int = 0,
    val emprendimientosPendientes: Int = 0,
    val productosActivos: Int = 0,
    val totalOrdenes: Int = 0,
    val montoTotalVentas: Double = 0.0
)
