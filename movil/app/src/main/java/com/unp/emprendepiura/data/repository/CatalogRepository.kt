package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.RealtimeChannelHelper
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
 * Archivo: CatalogRepository.kt
 * Proposito: Consultas publicas de catalogo, categorias, productos activos, emprendimientos
 *            aprobados y canal Realtime WSS para reflejar cambios de stock y publicaciones.
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

    /**
     * Suscripcion Realtime sobre productos y emprendimientos para mantener stock y
     * catalogo sincronizados en vivo sin recargas manuales.
     */
    fun observarCatalogoRealtime(): Flow<Unit> = callbackFlow {
        val channelId = RealtimeChannelHelper.uniqueChannelId("emprende_catalog")
        val channel = SupaClient.client.channel(channelId)

        val prodJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "productos"
            }.collect {
                trySend(Unit)
            }
        }

        val storeJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "emprendimientos"
            }.collect {
                trySend(Unit)
            }
        }

        val subJob = launch {
            RealtimeChannelHelper.suscribirConReintentos(channel) { esReconexion ->
                if (esReconexion) trySend(Unit)
            }
        }

        awaitClose {
            subJob.cancel()
            prodJob.cancel()
            storeJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                RealtimeChannelHelper.removerCanalSiCoincide(channel)
            }
        }
    }
}
