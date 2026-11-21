package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.ConversacionCreateDto
import com.unp.emprendepiura.data.dto.ConversacionDto
import com.unp.emprendepiura.data.dto.MensajeDto
import com.unp.emprendepiura.data.dto.MensajeInsertDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Mensaje
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/*
 * Archivo: ChatRepository.kt
 * Proposito: Mensajeria real comprador <-> emprendedor respaldada por PostgreSQL y
 *            escucha en tiempo real mediante Supabase Realtime WebSockets.
 */
class ChatRepository {

    suspend fun obtenerOCrearConversacion(
        compradorId: String,
        emprendimientoId: String,
        productoId: String? = null,
        ordenId: String? = null
    ): Conversacion {
        val existente = SupaClient.client.from("conversaciones")
            .select {
                filter {
                    eq("comprador_id", compradorId)
                    eq("emprendimiento_id", emprendimientoId)
                }
            }
            .decodeList<ConversacionDto>()
            .firstOrNull()

        if (existente != null) return existente.toDomain()

        val created = SupaClient.client.from("conversaciones")
            .insert(
                ConversacionCreateDto(
                    compradorId = compradorId,
                    emprendimientoId = emprendimientoId,
                    productoId = productoId,
                    ordenId = ordenId
                )
            ) { select() }
            .decodeSingle<ConversacionDto>()

        return created.toDomain()
    }

    suspend fun getConversacionesUsuario(usuarioId: String, miEmprendimientoId: String?): List<Conversacion> {
        val comoComprador = SupaClient.client.from("conversaciones")
            .select {
                filter { eq("comprador_id", usuarioId) }
                order("actualizado_en", order = Order.DESCENDING)
            }
            .decodeList<ConversacionDto>()

        val comoTienda = if (!miEmprendimientoId.isNullOrBlank()) {
            SupaClient.client.from("conversaciones")
                .select {
                    filter { eq("emprendimiento_id", miEmprendimientoId) }
                    order("actualizado_en", order = Order.DESCENDING)
                }
                .decodeList<ConversacionDto>()
        } else {
            emptyList()
        }

        return (comoComprador + comoTienda)
            .distinctBy { it.id }
            .sortedByDescending { it.actualizadoEn }
            .map { it.toDomain() }
    }

    suspend fun getMensajes(conversacionId: String): List<Mensaje> {
        return SupaClient.client.from("mensajes")
            .select {
                filter { eq("conversacion_id", conversacionId) }
                order("creado_en", order = Order.ASCENDING)
            }
            .decodeList<MensajeDto>()
            .map { it.toDomain() }
    }

    suspend fun enviarMensaje(conversacionId: String, remitenteId: String, contenido: String): Mensaje {
        val clean = contenido.trim()
        require(clean.isNotEmpty()) { "El mensaje no puede estar vacío." }

        return SupaClient.client.from("mensajes")
            .insert(MensajeInsertDto(conversacionId, remitenteId, clean)) { select() }
            .decodeSingle<MensajeDto>()
            .toDomain()
    }

    /** Observa mensajes nuevos insertados en la conversacion en tiempo real. */
    fun observarMensajesRealtime(conversacionId: String): Flow<Mensaje> = callbackFlow {
        val channelName = "chat_${conversacionId.take(12)}_${System.currentTimeMillis()}"
        val channel = SupaClient.client.channel(channelName)

        val insertFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "mensajes"
        }

        val collectJob = insertFlow.onEach { action ->
            runCatching { action.decodeRecord<MensajeDto>() }.getOrNull()?.let { dto ->
                if (dto.conversacionId == conversacionId) {
                    trySend(dto.toDomain())
                }
            }
        }.launchIn(this)

        launch {
            runCatching { channel.subscribe() }
        }

        awaitClose {
            collectJob.cancel()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching { channel.unsubscribe() }
            }
        }
    }
}
