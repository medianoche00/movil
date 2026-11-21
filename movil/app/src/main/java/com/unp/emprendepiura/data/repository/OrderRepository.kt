package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.OrdenDto
import com.unp.emprendepiura.data.dto.OrdenItemDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.CartItemLocal
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.MetodoPago
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.TipoEntrega
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Archivo: OrderRepository.kt
 * Proposito: Creacion atomica de ordenes mediante RPC con descuento de stock, subida de
 *            comprobantes Yape/Plin y seguimiento de pedidos para comprador y emprendedor.
 */
class OrderRepository {

    suspend fun subirComprobantePago(compradorId: String, imageBytes: ByteArray): String {
        val path = "comprobantes/$compradorId/${System.currentTimeMillis()}.jpg"
        SupaClient.client.storage.from(SupaClient.BUCKET_COMPROBANTES).upload(path, imageBytes) {
            upsert = true
        }
        return SupaClient.client.storage.from(SupaClient.BUCKET_COMPROBANTES).publicUrl(path)
    }

    suspend fun crearOrdenAtomica(
        compradorId: String,
        emprendimientoId: String,
        tipoEntrega: TipoEntrega,
        direccionOPunto: String,
        metodoPago: MetodoPago,
        comprobanteUrl: String?,
        codigoOperacion: String,
        notas: String,
        items: List<CartItemLocal>
    ): String {
        val itemsJson = buildJsonArray {
            items.forEach { item ->
                addJsonObject {
                    put("producto_id", item.producto.id)
                    put("cantidad", item.cantidad)
                }
            }
        }

        val params = buildJsonObject {
            put("p_comprador_id", compradorId)
            put("p_emprendimiento_id", emprendimientoId)
            put("p_tipo_entrega", tipoEntrega.name)
            put("p_direccion_punto", direccionOPunto.trim())
            put("p_metodo_pago", metodoPago.name)
            put("p_comprobante_url", comprobanteUrl)
            put("p_codigo_operacion", codigoOperacion.trim())
            put("p_notas", notas.trim())
            put("p_items", itemsJson)
        }

        return SupaClient.client.postgrest.rpc(
            function = "crear_orden_con_stock",
            parameters = params
        ).decodeAs<String>()
    }

    suspend fun getOrdenesComprador(compradorId: String): List<Orden> {
        val dtoList = SupaClient.client.from("ordenes")
            .select {
                filter { eq("comprador_id", compradorId) }
                order("creado_en", order = Order.DESCENDING)
            }
            .decodeList<OrdenDto>()

        return hidratarItems(dtoList)
    }

    suspend fun getOrdenesEmprendimiento(emprendimientoId: String): List<Orden> {
        val dtoList = SupaClient.client.from("ordenes")
            .select {
                filter { eq("emprendimiento_id", emprendimientoId) }
                order("creado_en", order = Order.DESCENDING)
            }
            .decodeList<OrdenDto>()

        return hidratarItems(dtoList)
    }

    suspend fun actualizarEstadoOrden(ordenId: String, nuevoEstado: EstadoOrden) {
        SupaClient.client.postgrest.rpc(
            function = "actualizar_estado_orden",
            parameters = buildJsonObject {
                put("p_orden_id", ordenId)
                put("p_nuevo_estado", nuevoEstado.name)
            }
        )
    }

    private suspend fun hidratarItems(ordenes: List<OrdenDto>): List<Orden> {
        if (ordenes.isEmpty()) return emptyList()
        val ids = ordenes.map { it.id }
        val itemsDto = SupaClient.client.from("orden_items")
            .select { filter { isIn("orden_id", ids) } }
            .decodeList<OrdenItemDto>()
            .groupBy { it.ordenId }

        return ordenes.map { o ->
            o.toDomain(
                items = itemsDto[o.id].orEmpty().map { it.toDomain() }
            )
        }
    }
}
