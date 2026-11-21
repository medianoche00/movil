package com.unp.emprendepiura.data.dto

import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.MetodoPago
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.OrdenItem
import com.unp.emprendepiura.domain.model.TipoEntrega
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrdenDto(
    val id: String = "",
    @SerialName("codigo_orden")
    val codigoOrden: String = "",
    @SerialName("comprador_id")
    val compradorId: String,
    @SerialName("emprendimiento_id")
    val emprendimientoId: String,
    @SerialName("tipo_entrega")
    val tipoEntrega: String,
    @SerialName("direccion_o_punto")
    val direccionOPunto: String,
    @SerialName("metodo_pago")
    val metodoPago: String,
    @SerialName("comprobante_url")
    val comprobanteUrl: String? = null,
    @SerialName("codigo_operacion")
    val codigoOperacion: String? = null,
    val total: Double = 0.0,
    val estado: String = "PENDIENTE",
    @SerialName("notas_comprador")
    val notasComprador: String = "",
    @SerialName("creado_en")
    val creadoEn: String = ""
)

@Serializable
data class OrdenItemDto(
    val id: String = "",
    @SerialName("orden_id")
    val ordenId: String,
    @SerialName("producto_id")
    val productoId: String,
    @SerialName("titulo_snapshot")
    val tituloSnapshot: String,
    @SerialName("precio_unitario")
    val precioUnitario: Double,
    val cantidad: Int,
    val subtotal: Double
)

fun OrdenItemDto.toDomain() = OrdenItem(
    id = id,
    ordenId = ordenId,
    productoId = productoId,
    tituloSnapshot = tituloSnapshot,
    precioUnitario = precioUnitario,
    cantidad = cantidad,
    subtotal = subtotal
)

fun OrdenDto.toDomain(
    nombreTienda: String = "",
    nombreComprador: String = "",
    items: List<OrdenItem> = emptyList()
) = Orden(
    id = id,
    codigoOrden = codigoOrden,
    compradorId = compradorId,
    emprendimientoId = emprendimientoId,
    tipoEntrega = TipoEntrega.fromRaw(tipoEntrega),
    direccionOPunto = direccionOPunto,
    metodoPago = MetodoPago.fromRaw(metodoPago),
    comprobanteUrl = comprobanteUrl.orEmpty(),
    codigoOperacion = codigoOperacion.orEmpty(),
    total = total,
    estado = EstadoOrden.fromRaw(estado),
    notasComprador = notasComprador,
    creadoEn = creadoEn,
    nombreTienda = nombreTienda,
    nombreComprador = nombreComprador,
    items = items
)
