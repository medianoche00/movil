package com.unp.emprendepiura.data.dto

import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Mensaje
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversacionDto(
    val id: String = "",
    @SerialName("comprador_id")
    val compradorId: String = "",
    @SerialName("emprendimiento_id")
    val emprendimientoId: String = "",
    @SerialName("producto_id")
    val productoId: String? = null,
    @SerialName("orden_id")
    val ordenId: String? = null,
    @SerialName("ultimo_mensaje")
    val ultimoMensaje: String = "",
    @SerialName("actualizado_en")
    val actualizadoEn: String = "",
    @SerialName("bloqueado_por")
    val bloqueadoPor: String? = null,
    @SerialName("bloqueado_en")
    val bloqueadoEn: String? = null
)

@Serializable
data class ConversacionCreateDto(
    @SerialName("comprador_id")
    val compradorId: String,
    @SerialName("emprendimiento_id")
    val emprendimientoId: String,
    @SerialName("producto_id")
    val productoId: String? = null,
    @SerialName("orden_id")
    val ordenId: String? = null
)

@Serializable
data class MensajeDto(
    val id: String = "",
    @SerialName("conversacion_id")
    val conversacionId: String = "",
    @SerialName("remitente_id")
    val remitenteId: String = "",
    val contenido: String = "",
    val leido: Boolean = false,
    @SerialName("creado_en")
    val creadoEn: String = ""
)

@Serializable
data class MensajeInsertDto(
    @SerialName("conversacion_id")
    val conversacionId: String,
    @SerialName("remitente_id")
    val remitenteId: String,
    val contenido: String
)

@Serializable
data class TypingBroadcastDto(
    @SerialName("usuario_id")
    val usuarioId: String,
    val escribiendo: Boolean
)

@Serializable
data class ChatPresenceDto(
    @SerialName("usuario_id")
    val usuarioId: String
)

fun ConversacionDto.toDomain(
    nombreContraparte: String = "",
    subtituloProducto: String = "",
    mensajesNoLeidos: Int = 0
) = Conversacion(
    id = id,
    compradorId = compradorId,
    emprendimientoId = emprendimientoId,
    productoId = productoId,
    ordenId = ordenId,
    ultimoMensaje = ultimoMensaje,
    actualizadoEn = actualizadoEn,
    bloqueadoPor = bloqueadoPor,
    bloqueadoEn = bloqueadoEn,
    mensajesNoLeidos = mensajesNoLeidos,
    nombreContraparte = nombreContraparte,
    subtituloProducto = subtituloProducto
)

fun MensajeDto.toDomain() = Mensaje(
    id = id,
    conversacionId = conversacionId,
    remitenteId = remitenteId,
    contenido = contenido,
    leido = leido,
    creadoEn = creadoEn
)
