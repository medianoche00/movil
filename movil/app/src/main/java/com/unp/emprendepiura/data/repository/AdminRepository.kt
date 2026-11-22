package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.RealtimeChannelHelper
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
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.model.Usuario
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/*
 * Archivo: AdminRepository.kt
 * Proposito: Operaciones administrativas: gestion de usuarios (habilitar/deshabilitar),
 *            aprobacion/rechazo de emprendimientos, categorias, moderacion de productos,
 *            seguimiento global de ordenes y estadisticas reales de plataforma.
 */
class AdminRepository {

    data class AdminSnapshot(
        val stats: AdminDashboardStats,
        val usuarios: List<Usuario>,
        val emprendimientos: List<Emprendimiento>,
        val categorias: List<Categoria>,
        val productos: List<Producto>,
        val ordenes: List<Orden>
    )

    suspend fun getSnapshotCompleto(): AdminSnapshot {
        val usuariosDto = SupaClient.client.from("usuarios")
            .select { order("creado_en", order = Order.DESCENDING) }
            .decodeList<UsuarioRowDto>()
        val tiendasDto = SupaClient.client.from("emprendimientos")
            .select { order("creado_en", order = Order.DESCENDING) }
            .decodeList<EmprendimientoDto>()
        val categoriasDto = SupaClient.client.from("categorias")
            .select { order("orden", order = Order.ASCENDING) }
            .decodeList<CategoriaDto>()
        val productosDto = SupaClient.client.from("productos")
            .select { order("creado_en", order = Order.DESCENDING) }
            .decodeList<ProductoDto>()
        val ordenesDto = SupaClient.client.from("ordenes")
            .select { order("creado_en", order = Order.DESCENDING) }
            .decodeList<OrdenDto>()

        val usuarios = usuariosDto.map { it.toDomain() }
        val tiendas = tiendasDto.map { it.toDomain() }
            .sortedBy {
                when (it.estado) {
                    EstadoEmprendimiento.PENDIENTE -> 0
                    EstadoEmprendimiento.APROBADO -> 1
                    EstadoEmprendimiento.SUSPENDIDO -> 2
                }
            }
        val tiendasMap = tiendas.associateBy { it.id }
        val usuariosMap = usuarios.associateBy { it.id }
        val categorias = categoriasDto.map { it.toDomain() }
        val categoriasMap = categorias.associateBy { it.id }

        val productos = productosDto.map { dto ->
            dto.toDomain(
                nombreTienda = tiendasMap[dto.emprendimientoId]?.nombreTienda ?: "Emprendimiento",
                nombreCategoria = categoriasMap[dto.categoriaId]?.nombre ?: "General"
            )
        }

        val ordenes = ordenesDto.map { dto ->
            dto.toDomain().copy(
                nombreTienda = tiendasMap[dto.emprendimientoId]?.nombreTienda ?: "Emprendimiento",
                nombreComprador = usuariosMap[dto.compradorId]?.nombre ?: "Comprador"
            )
        }

        val stats = AdminDashboardStats(
            totalUsuarios = usuarios.size,
            emprendimientosAprobados = tiendas.count { it.estado == EstadoEmprendimiento.APROBADO },
            emprendimientosPendientes = tiendas.count { it.estado == EstadoEmprendimiento.PENDIENTE },
            productosActivos = productos.count { it.estado == EstadoProducto.ACTIVO },
            totalOrdenes = ordenes.size,
            montoTotalVentas = ordenesDto
                .filter { it.estado != "CANCELADO" }
                .sumOf { it.total }
        )

        return AdminSnapshot(
            stats = stats,
            usuarios = usuarios,
            emprendimientos = tiendas,
            categorias = categorias,
            productos = productos,
            ordenes = ordenes
        )
    }

    suspend fun cambiarEstadoActivoUsuario(usuarioId: String, activo: Boolean) {
        SupaClient.client.from("usuarios").update(mapOf("activo" to activo)) {
            filter { eq("id", usuarioId) }
        }
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

    suspend fun moderarProducto(productoId: String, estado: EstadoProducto) {
        SupaClient.client.from("productos").update(mapOf("estado" to estado.name)) {
            filter { eq("id", productoId) }
        }
    }

    /**
     * Observa cambios en emprendimientos, ordenes y productos para mantener
     * actualizado el Panel Administrativo en tiempo real cuando llegan solicitudes.
     */
    fun observarCambiosAdminRealtime(): Flow<Unit> = callbackFlow {
        val channelId = RealtimeChannelHelper.uniqueChannelId("emprende_admin_live")
        val channel = SupaClient.client.channel(channelId)

        val storesJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "emprendimientos"
            }.collect { trySend(Unit) }
        }
        val ordersJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "ordenes"
            }.collect { trySend(Unit) }
        }
        val productsJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "productos"
            }.collect { trySend(Unit) }
        }

        val subJob = launch {
            RealtimeChannelHelper.suscribirConReintentos(channel) { esReconexion ->
                if (esReconexion) trySend(Unit)
            }
        }

        awaitClose {
            subJob.cancel()
            storesJob.cancel()
            ordersJob.cancel()
            productsJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                RealtimeChannelHelper.removerCanalSiCoincide(channel)
            }
        }
    }
}
