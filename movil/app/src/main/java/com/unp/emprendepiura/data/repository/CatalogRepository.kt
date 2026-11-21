package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.CategoriaDto
import com.unp.emprendepiura.data.dto.EmprendimientoDto
import com.unp.emprendepiura.data.dto.ProductoDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Producto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/*
 * Archivo: CatalogRepository.kt
 * Proposito: Consultas publicas de catalogo, categorias, productos activos y emprendimientos aprobados.
 */
class CatalogRepository {

    suspend fun getCategoriasActivas(): List<Categoria> {
        return SupaClient.client.from("categorias")
            .select {
                filter { eq("activa", true) }
                order("orden", order = Order.ASCENDING)
            }
            .decodeList<CategoriaDto>()
            .map { it.toDomain() }
    }

    suspend fun getEmprendimientosAprobados(): List<Emprendimiento> {
        return SupaClient.client.from("emprendimientos")
            .select {
                filter { eq("estado", "APROBADO") }
                order("nombre_tienda", order = Order.ASCENDING)
            }
            .decodeList<EmprendimientoDto>()
            .map { it.toDomain() }
    }

    suspend fun getProductosActivos(): List<Producto> {
        val categorias = getCategoriasActivas().associateBy { it.id }
        val tiendas = getEmprendimientosAprobados().associateBy { it.id }

        return SupaClient.client.from("productos")
            .select {
                filter { eq("estado", "ACTIVO") }
                order("destacado", order = Order.DESCENDING)
                order("creado_en", order = Order.DESCENDING)
            }
            .decodeList<ProductoDto>()
            .filter { tiendas.containsKey(it.emprendimientoId) }
            .map { dto ->
                dto.toDomain(
                    nombreTienda = tiendas[dto.emprendimientoId]?.nombreTienda ?: "Emprendimiento Local",
                    nombreCategoria = categorias[dto.categoriaId]?.nombre ?: "General"
                )
            }
    }

    suspend fun getEmprendimientoPorId(id: String): Emprendimiento? {
        return SupaClient.client.from("emprendimientos")
            .select { filter { eq("id", id) } }
            .decodeList<EmprendimientoDto>()
            .firstOrNull()
            ?.toDomain()
    }
}
