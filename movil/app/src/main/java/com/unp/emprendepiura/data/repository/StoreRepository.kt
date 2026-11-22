package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.RealtimeChannelHelper
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
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/*
 * Archivo: StoreRepository.kt
 * Proposito: Gestion del perfil de emprendimiento del usuario, reenvio de solicitudes rechazadas
 *            (forzando estado = PENDIENTE), productos propios y suscripcion Realtime de estado.
 */
class StoreRepository {

    suspend fun getMiEmprendimiento(usuarioId: String): Emprendimiento? {
        return SupaClient.client.from("emprendimientos")
            .select { filter { eq("usuario_id", usuarioId) } }
            .decodeList<EmprendimientoDto>()
            .firstOrNull()
            ?.toDomain()
    }

    /**
     * Crea una solicitud nueva o actualiza una solicitud rechazada previa,
     * garantizando siempre que el estado pase explicitamente a PENDIENTE para
     * aparecer de inmediato en el Panel Administrativo.
     */
    suspend fun registrarOSolicitarTienda(data: EmprendimientoWriteDto): Emprendimiento {
        val existente = getMiEmprendimiento(data.usuarioId)
        val dtoFinal = if (existente != null) {
            SupaClient.client.from("emprendimientos")
                .update({
                    set("nombre_tienda", data.nombreTienda)
                    set("descripcion", data.descripcion)
                    set("categoria_principal", data.categoriaPrincipal)
                    set("zona_piura", data.zonaPiura)
                    set("punto_encuentro_habitual", data.puntoEncuentroHabitual)
                    set("telefono_contacto", data.telefonoContacto)
                    set("numero_yape_plin", data.numeroYapePlin)
                    set("estado", "PENDIENTE")
                    set("motivo_revision", "")
                }) {
                    filter { eq("usuario_id", data.usuarioId) }
                    select()
                }
                .decodeSingle<EmprendimientoDto>()
        } else {
            SupaClient.client.from("emprendimientos")
                .insert(data.copy(estado = "PENDIENTE", motivoRevision = "")) {
                    select()
                }
                .decodeSingle<EmprendimientoDto>()
        }
        return dtoFinal.toDomain()
    }

    /**
     * Observa cambios en tiempo real sobre el emprendimiento del usuario
     * (por ejemplo cuando el administrador aprueba o rechaza su solicitud).
     */
    fun observarMiEmprendimientoRealtime(usuarioId: String): Flow<Unit> = callbackFlow {
        val channelId = RealtimeChannelHelper.uniqueChannelId("emprende_mystore_${usuarioId.take(10)}")
        val channel = SupaClient.client.channel(channelId)

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "emprendimientos"
            }.collect { action ->
                val relevante = when (action) {
                    is PostgresAction.Insert -> action.decodeRecordOrNull<EmprendimientoDto>()?.usuarioId == usuarioId
                    is PostgresAction.Update -> action.decodeRecordOrNull<EmprendimientoDto>()?.usuarioId == usuarioId
                    else -> true
                }
                if (relevante) trySend(Unit)
            }
        }

        val subJob = launch {
            RealtimeChannelHelper.suscribirConReintentos(channel) { esReconexion ->
                if (esReconexion) trySend(Unit)
            }
        }

        awaitClose {
            subJob.cancel()
            job.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                RealtimeChannelHelper.removerCanalSiCoincide(channel)
            }
        }
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
