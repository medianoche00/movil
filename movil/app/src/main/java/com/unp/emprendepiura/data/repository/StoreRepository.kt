package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.EmprendimientoDto
import com.unp.emprendepiura.data.dto.EmprendimientoWriteDto
import com.unp.emprendepiura.data.dto.ProductoDto
import com.unp.emprendepiura.data.dto.ProductoWriteDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage

/*
 * Archivo: StoreRepository.kt
 * Proposito: Gestion del perfil de emprendimiento del usuario y sus productos propios en Supabase.
 */
class StoreRepository {

    suspend fun getMiEmprendimiento(usuarioId: String): Emprendimiento? {
        return SupaClient.client.from("emprendimientos")
            .select { filter { eq("usuario_id", usuarioId) } }
            .decodeList<EmprendimientoDto>()
            .firstOrNull()
            ?.toDomain()
    }

    suspend fun registrarOSolicitarTienda(data: EmprendimientoWriteDto): Emprendimiento {
        return SupaClient.client.from("emprendimientos")
            .upsert(data) {
                onConflict = "usuario_id"
                select()
            }
            .decodeSingle<EmprendimientoDto>()
            .toDomain()
    }

    suspend fun getProductosDeTienda(emprendimientoId: String): List<Producto> {
        return SupaClient.client.from("productos")
            .select {
                filter { eq("emprendimiento_id", emprendimientoId) }
                order("creado_en", order = Order.DESCENDING)
            }
            .decodeList<ProductoDto>()
            .map { it.toDomain() }
    }

    suspend fun crearProducto(data: ProductoWriteDto): Producto {
        return SupaClient.client.from("productos")
            .insert(data) { select() }
            .decodeSingle<ProductoDto>()
            .toDomain()
    }

    suspend fun actualizarProducto(productoId: String, data: ProductoWriteDto): Producto {
        return SupaClient.client.from("productos")
            .update(data) {
                select()
                filter { eq("id", productoId) }
            }
            .decodeSingle<ProductoDto>()
            .toDomain()
    }

    suspend fun cambiarEstadoProducto(productoId: String, estado: EstadoProducto) {
        SupaClient.client.from("productos").update(mapOf("estado" to estado.name)) {
            filter { eq("id", productoId) }
        }
    }

    suspend fun subirImagenProducto(emprendimientoId: String, bytes: ByteArray): String {
        val path = "productos/$emprendimientoId/${System.currentTimeMillis()}.jpg"
        SupaClient.client.storage.from(SupaClient.BUCKET_PRODUCTOS).upload(path, bytes) {
            upsert = true
        }
        return SupaClient.client.storage.from(SupaClient.BUCKET_PRODUCTOS).publicUrl(path)
    }
}
