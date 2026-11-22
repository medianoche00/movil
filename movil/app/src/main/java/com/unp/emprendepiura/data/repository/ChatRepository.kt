package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.ChatPresenceDto
import com.unp.emprendepiura.data.dto.ConversacionCreateDto
import com.unp.emprendepiura.data.dto.ConversacionDto
import com.unp.emprendepiura.data.dto.MensajeDto
import com.unp.emprendepiura.data.dto.MensajeInsertDto
import com.unp.emprendepiura.data.dto.TypingBroadcastDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.ChatRoomRealtimeEvent
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Mensaje
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeJoinsAs
import io.github.jan.supabase.realtime.decodeLeavesAs
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/*
 * Archivo: ChatRepository.kt
 * Proposito: Mensajeria real comprador <-> emprendedor con persistencia en PostgreSQL,
 *            bloqueo bidireccional persistente, confirmacion de lectura (leido=true) y
 *            Supabase Realtime (Postgres Changes, Broadcast de escritura y Presence en vivo).
 */
class ChatRepository {

    private val canalesSala = ConcurrentHashMap<String, RealtimeChannel>()

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

        val unicas = (comoComprador + comoTienda)
            .distinctBy { it.id }
            .sortedByDescending { it.actualizadoEn }

        val conteoNoLeidos = obtenerConteoNoLeidos(unicas.map { it.id }, usuarioId)

        return unicas.map { dto ->
            dto.toDomain(mensajesNoLeidos = conteoNoLeidos[dto.id] ?: 0)
        }
    }

    private suspend fun obtenerConteoNoLeidos(conversacionIds: List<String>, miUsuarioId: String): Map<String, Int> {
        if (conversacionIds.isEmpty() || miUsuarioId.isBlank()) return emptyMap()
        val idSet = conversacionIds.toSet()
        return runCatching {
            SupaClient.client.from("mensajes")
                .select {
                    filter {
                        eq("leido", false)
                        neq("remitente_id", miUsuarioId)
                    }
                }
                .decodeList<MensajeDto>()
                .asSequence()
                .filter { it.conversacionId in idSet }
                .groupingBy { it.conversacionId }
                .eachCount()
        }.getOrDefault(emptyMap())
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

    suspend fun marcarMensajesComoLeidos(conversacionId: String, miUsuarioId: String) {
        if (conversacionId.isBlank() || miUsuarioId.isBlank()) return
        runCatching {
            SupaClient.client.from("mensajes")
                .update({
                    set("leido", true)
                }) {
                    filter {
                        eq("conversacion_id", conversacionId)
                        neq("remitente_id", miUsuarioId)
                        eq("leido", false)
                    }
                }
        }
    }

    suspend fun actualizarBloqueoConversacion(conversacionId: String, bloqueadoPor: String?): Conversacion {
        val updated = SupaClient.client.from("conversaciones")
            .update({
                if (bloqueadoPor.isNullOrBlank()) {
                    setToNull("bloqueado_por")
                    setToNull("bloqueado_en")
                } else {
                    set("bloqueado_por", bloqueadoPor)
                    set("bloqueado_en", Instant.now().toString())
                }
            }) {
                filter { eq("id", conversacionId) }
                select()
            }
            .decodeSingle<ConversacionDto>()

        return updated.toDomain()
    }

    suspend fun eliminarConversacion(conversacionId: String) {
        runCatching {
            SupaClient.client.from("mensajes").delete {
                filter { eq("conversacion_id", conversacionId) }
            }
        }
        SupaClient.client.from("conversaciones").delete {
            filter { eq("id", conversacionId) }
        }
    }

    suspend fun enviarIndicadorEscritura(conversacionId: String, miUsuarioId: String, escribiendo: Boolean) {
        val channel = canalesSala[conversacionId] ?: return
        if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) return
        runCatching {
            channel.broadcast(
                event = "typing",
                message = TypingBroadcastDto(usuarioId = miUsuarioId, escribiendo = escribiendo)
            )
        }
    }

    /**
     * Canal Realtime unificado de sala de chat:
     * - Nuevos mensajes y confirmacion de lectura (leido=true) via Postgres Changes.
     * - Bloqueo/desbloqueo y eliminacion de conversacion en tiempo real.
     * - Indicador real de "escribiendo..." via Broadcast.
     * - Presencia real "En linea" via Presence.
     */
    fun observarSalaRealtime(conversacionId: String, miUsuarioId: String): Flow<ChatRoomRealtimeEvent> = callbackFlow {
        val channelId = "emprende_room_$conversacionId"

        // Limpiar cualquier instancia residual del mismo canal antes de registrar flujos
        canalesSala.remove(conversacionId)?.let { previo ->
            runCatching { SupaClient.client.realtime.removeChannel(previo) }
        }
        SupaClient.client.realtime.subscriptions.values
            .firstOrNull { it.topic.endsWith(channelId) }
            ?.let { residual ->
                runCatching { SupaClient.client.realtime.removeChannel(residual) }
            }

        val channel = SupaClient.client.channel(channelId) {
            broadcast { receiveOwnBroadcasts = false }
            presence { key = miUsuarioId }
        }
        canalesSala[conversacionId] = channel

        val mensajesJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "mensajes"
        }.onEach { action ->
            when (action) {
                is PostgresAction.Insert -> {
                    action.decodeRecordOrNull<MensajeDto>()?.let { dto ->
                        if (dto.conversacionId == conversacionId) {
                            trySend(ChatRoomRealtimeEvent.NuevoMensaje(dto.toDomain()))
                        }
                    }
                }
                is PostgresAction.Update -> {
                    action.decodeRecordOrNull<MensajeDto>()?.let { dto ->
                        if (dto.conversacionId == conversacionId) {
                            trySend(ChatRoomRealtimeEvent.MensajeActualizado(dto.toDomain()))
                        }
                    }
                }
                else -> Unit
            }
        }.launchIn(this)

        val conversacionJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "conversaciones"
        }.onEach { action ->
            when (action) {
                is PostgresAction.Update -> {
                    action.decodeRecordOrNull<ConversacionDto>()?.let { dto ->
                        if (dto.id == conversacionId) {
                            trySend(ChatRoomRealtimeEvent.ConversacionActualizada(dto.toDomain()))
                        }
                    }
                }
                is PostgresAction.Delete -> {
                    action.decodeOldRecordOrNull<ConversacionDto>()?.let { dto ->
                        if (dto.id == conversacionId) {
                            trySend(ChatRoomRealtimeEvent.ConversacionEliminada(dto.id))
                        }
                    }
                }
                else -> Unit
            }
        }.launchIn(this)

        val typingJob = channel.broadcastFlow<TypingBroadcastDto>(event = "typing")
            .onEach { dto ->
                if (dto.usuarioId != miUsuarioId) {
                    trySend(ChatRoomRealtimeEvent.ContraparteEscribiendo(dto.escribiendo))
                }
            }
            .launchIn(this)

        val usuariosPresentes = mutableSetOf<String>()
        val presenceJob = channel.presenceChangeFlow()
            .onEach { action ->
                action.decodeJoinsAs<ChatPresenceDto>().forEach { usuariosPresentes.add(it.usuarioId) }
                action.decodeLeavesAs<ChatPresenceDto>().forEach { usuariosPresentes.remove(it.usuarioId) }
                trySend(ChatRoomRealtimeEvent.ContraparteEnLinea(usuariosPresentes.any { it != miUsuarioId }))
            }
            .launchIn(this)

        val subscribeJob = launch {
            runCatching {
                channel.subscribe(blockUntilSubscribed = true)
                channel.track(buildJsonObject { put("usuario_id", miUsuarioId) })
            }
        }

        awaitClose {
            canalesSala.remove(conversacionId, channel)
            subscribeJob.cancel()
            mensajesJob.cancel()
            conversacionJob.cancel()
            typingJob.cancel()
            presenceJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { channel.untrack() }
                runCatching { SupaClient.client.realtime.removeChannel(channel) }
            }
        }
    }

    /**
     * Observa cambios globales en conversaciones y mensajes para refrescar la bandeja
     * y los contadores de no leidos en tiempo real.
     */
    fun observarBandejaRealtime(usuarioId: String, miEmprendimientoId: String?): Flow<Unit> = callbackFlow {
        val channelId = "emprende_inbox_${usuarioId.take(12)}"

        SupaClient.client.realtime.subscriptions.values
            .firstOrNull { it.topic.endsWith(channelId) }
            ?.let { residual ->
                runCatching { SupaClient.client.realtime.removeChannel(residual) }
            }

        val channel = SupaClient.client.channel(channelId)

        val convJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "conversaciones"
        }.onEach { action ->
            val relevante = when (action) {
                is PostgresAction.Insert -> action.decodeRecordOrNull<ConversacionDto>()?.let {
                    it.compradorId == usuarioId || (!miEmprendimientoId.isNullOrBlank() && it.emprendimientoId == miEmprendimientoId)
                } ?: true
                is PostgresAction.Update -> action.decodeRecordOrNull<ConversacionDto>()?.let {
                    it.compradorId == usuarioId || (!miEmprendimientoId.isNullOrBlank() && it.emprendimientoId == miEmprendimientoId)
                } ?: true
                is PostgresAction.Delete -> true
                else -> false
            }
            if (relevante) trySend(Unit)
        }.launchIn(this)

        val msgJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "mensajes"
        }.onEach { action ->
            if (action is PostgresAction.Insert || action is PostgresAction.Update) {
                trySend(Unit)
            }
        }.launchIn(this)

        val subJob = launch {
            runCatching { channel.subscribe() }
        }

        awaitClose {
            subJob.cancel()
            convJob.cancel()
            msgJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { SupaClient.client.realtime.removeChannel(channel) }
            }
        }
    }
}
