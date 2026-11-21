package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.CategoriaDto
import com.unp.emprendepiura.data.dto.CategoriaWriteDto
import com.unp.emprendepiura.data.dto.EmprendimientoDto
import com.unp.emprendepiura.data.dto.OrdenDto
import com.unp.emprendepiura.data.dto.ProductoDto
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.AdminDashboardStats
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/*
 * Archivo: AdminRepository.kt
 * Proposito: Operaciones administrativas: aprobacion/suspension de emprendimientos,
 *            gestion de categorias, moderacion de productos y estadisticas reales de plataforma.
 */
class AdminRepository {

    suspend fun getDashboardStats(): AdminDashboardStats {
        val usuarios = SupaClient.client.from("usuarios").select().decodeList<UsuarioRowDto>()
        val tiendas = SupaClient.client.from("emprendimientos").select().decodeList<EmprendimientoDto>()
        val productos = SupaClient.client.from("productos").select().decodeList<ProductoDto>()
        val ordenes = SupaClient.client.from("ordenes").select().decodeList<OrdenDto>()

        return AdminDashboardStats(
            totalUsuarios = usuarios.size,
            emprendimientosAprobados = tiendas.count { it.estado == EstadoEmprendimiento.APROBADO.name },
            emprendimientosPendientes = tiendas.count { it.estado == EstadoEmprendimiento.PENDIENTE.name },
            productosActivos = productos.count { it.estado == EstadoProducto.ACTIVO.name },
            totalOrdenes = ordenes.size,
            montoTotalVentas = ordenes
                .filter { it.estado != "CANCELADO" }
                .sumOf { it.total }
        )
    }

    suspend fun getTodosEmprendimientos(): List<Emprendimiento> {
        return SupaClient.client.from("emprendimientos")
            .select { order("creado_en", order = Order.DESCENDING) }
            .decodeList<EmprendimientoDto>()
            .map { it.toDomain() }
    }

    suspend fun resolverEmprendimiento(
        emprendimientoId: String,
        nuevoEstado: EstadoEmprendimiento,
        motivo: String = ""
    ) {
        SupaClient.client.from("emprendimientos").update(
            mapOf(
                "estado" to nuevoEstado.name,
                "motivo_revision" to motivo.trim()
            )
        ) {
            filter { eq("id", emprendimientoId) }
        }
    }

    suspend fun getTodasCategorias(): List<Categoria> {
        return SupaClient.client.from("categorias")
            .select { order("orden", order = Order.ASCENDING) }
            .decodeList<CategoriaDto>()
            .map { it.toDomain() }
    }

    suspend fun crearCategoria(nombre: String, orden: Int): Categoria {
        return SupaClient.client.from("categorias")
            .insert(CategoriaWriteDto(nombre = nombre.trim(), orden = orden, activa = true)) { select() }
            .decodeSingle<CategoriaDto>()
            .toDomain()
    }

    suspend fun toggleCategoriaActiva(categoriaId: String, activa: Boolean) {
        SupaClient.client.from("categorias").update(mapOf("activa" to activa)) {
            filter { eq("id", categoriaId) }
        }
    }

    suspend fun getTodosProductosParaModeracion(): List<Producto> {
        return SupaClient.client.from("productos")
            .select { order("creado_en", order = Order.DESCENDING) }
            .decodeList<ProductoDto>()
            .map { it.toDomain() }
    }

    suspend fun moderarProducto(productoId: String, estado: EstadoProducto) {
        SupaClient.client.from("productos").update(mapOf("estado" to estado.name)) {
            filter { eq("id", productoId) }
        }
    }
}
