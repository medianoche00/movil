package com.unp.emprendepiura.data.dto

import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Mensaje
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversacionDto(
    val id: String = "",
    @SerialName("comprador_id")
    val compradorId: String,
    @SerialName("emprendimiento_id")
    val emprendimientoId: String,
    @SerialName("producto_id")
    val productoId: String? = null,
    @SerialName("orden_id")
    val ordenId: String? = null,
    @SerialName("ultimo_mensaje")
    val ultimoMensaje: String = "",
    @SerialName("actualizado_en")
    val actualizadoEn: String = ""
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
    val conversacionId: String,
    @SerialName("remitente_id")
    val remitenteId: String,
    val contenido: String,
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

fun ConversacionDto.toDomain(
    nombreContraparte: String = "",
    subtituloProducto: String = ""
) = Conversacion(
    id = id,
    compradorId = compradorId,
    emprendimientoId = emprendimientoId,
    productoId = productoId,
    ordenId = ordenId,
    ultimoMensaje = ultimoMensaje,
    actualizadoEn = actualizadoEn,
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
