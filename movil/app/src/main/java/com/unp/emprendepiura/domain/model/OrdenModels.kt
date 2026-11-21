package com.unp.emprendepiura.domain.model

/*
 * Archivo: OrdenModels.kt
 * Proposito: Modelos de dominio para Carrito, Ordenes y seguimiento de pedidos.
 */
enum class TipoEntrega(val etiqueta: String) {
    PUNTO_ENCUENTRO("Punto de encuentro (UNP / Piura)"),
    DELIVERY("Delivery a domicilio");

    companion object {
        fun fromRaw(raw: String): TipoEntrega =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PUNTO_ENCUENTRO
    }
}

enum class MetodoPago(val etiqueta: String) {
    CONTRA_ENTREGA("Contra entrega (Efectivo / Yape al recibir)"),
    YAPE_PLIN("Yape / Plin con comprobante");

    companion object {
        fun fromRaw(raw: String): MetodoPago =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: CONTRA_ENTREGA
    }
}

enum class EstadoOrden(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADO("Confirmado"),
    EN_CAMINO("Listo / En camino"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    companion object {
        fun fromRaw(raw: String): EstadoOrden =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PENDIENTE
    }
}

data class CartItemLocal(
    val producto: Producto,
    val cantidad: Int = 1
) {
    val subtotal: Double get() = producto.precio * cantidad
}

data class OrdenItem(
    val id: String = "",
    val ordenId: String = "",
    val productoId: String,
    val tituloSnapshot: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val subtotal: Double
)

data class Orden(
    val id: String = "",
    val codigoOrden: String = "",
    val compradorId: String,
    val emprendimientoId: String,
    val tipoEntrega: TipoEntrega,
    val direccionOPunto: String,
    val metodoPago: MetodoPago,
    val comprobanteUrl: String = "",
    val codigoOperacion: String = "",
    val total: Double,
    val estado: EstadoOrden = EstadoOrden.PENDIENTE,
    val notasComprador: String = "",
    val creadoEn: String = "",
    val nombreTienda: String = "",
    val nombreComprador: String = "",
    val items: List<OrdenItem> = emptyList()
)
