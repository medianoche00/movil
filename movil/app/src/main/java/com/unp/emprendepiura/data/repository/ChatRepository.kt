package com.unp.emprendepiura.data.repository

import com.unp.emprendepiura.core.network.RealtimeChannelHelper
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.ChatPresenceDto
import com.unp.emprendepiura.data.dto.ConversacionCreateDto
import com.unp.emprendepiura.data.dto.ConversacionDto
import com.unp.emprendepiura.data.dto.MensajeDto
import com.unp.emprendepiura.data.dto.MensajeInsertDto
import com.unp.emprendepiura.data.dto.TypingBroadcastDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.ChatInboxRealtimeEvent
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/*
 * Archivo: ChatRepository.kt
 * Proposito: Mensajeria real comprador <-> emprendedor con persistencia en PostgreSQL,
 *            bloqueo bidireccional persistente, confirmacion de lectura (leido=true) y
 *            Supabase Realtime resiliente (Postgres Changes, Broadcast de escritura y Presence)
 *            con registro deterministico UNDISPATCHED y reconexion entre redes distintas.
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

    suspend fun getConversacionPorId(conversacionId: String): Conversacion? {
        if (conversacionId.isBlank()) return null
        return SupaClient.client.from("conversaciones")
            .select { filter { eq("id", conversacionId) } }
            .decodeList<ConversacionDto>()
            .firstOrNull()
            ?.toDomain()
    }

    suspend fun existeBloqueoConTienda(compradorId: String, emprendimientoId: String): Boolean {
        if (compradorId.isBlank() || emprendimientoId.isBlank()) return false
        return runCatching {
            val conv = SupaClient.client.from("conversaciones")
                .select {
                    filter {
                        eq("comprador_id", compradorId)
                        eq("emprendimiento_id", emprendimientoId)
                    }
                }
                .decodeList<ConversacionDto>()
                .firstOrNull()
            !conv?.bloqueadoPor.isNullOrBlank()
        }.getOrDefault(false)
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

    /**
     * Sube un adjunto de chat (foto, nota de voz o documento) a Supabase Storage.
     * Intenta primero el bucket dedicado marketplace-chat y respalda automaticamente
     * en marketplace-productos/chat/ si el bucket dedicado aun no fue creado.
     */
    suspend fun subirAdjuntoChat(
        conversacionId: String,
        bytes: ByteArray,
        extension: String,
        carpeta: String
    ): String {
        val extSegura = extension.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "bin" }
        val fileName = "${System.currentTimeMillis()}_${(1000..9999).random()}.$extSegura"
        val primaryPath = "$carpeta/$conversacionId/$fileName"

        val primaryUpload = runCatching {
            SupaClient.client.storage.from(SupaClient.BUCKET_CHAT).upload(primaryPath, bytes) {
                upsert = true
            }
            SupaClient.client.storage.from(SupaClient.BUCKET_CHAT).publicUrl(primaryPath)
        }
        if (primaryUpload.isSuccess) {
            return primaryUpload.getOrThrow()
        }

        val fallbackPath = "chat/$carpeta/$conversacionId/$fileName"
        SupaClient.client.storage.from(SupaClient.BUCKET_PRODUCTOS).upload(fallbackPath, bytes) {
            upsert = true
        }
        return SupaClient.client.storage.from(SupaClient.BUCKET_PRODUCTOS).publicUrl(fallbackPath)
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

    suspend fun actualizarContenidoMensaje(mensajeId: String, nuevoContenido: String): Mensaje {
        return SupaClient.client.from("mensajes")
            .update({
                set("contenido", nuevoContenido)
            }) {
                filter { eq("id", mensajeId) }
                select()
            }
            .decodeSingle<MensajeDto>()
            .toDomain()
    }

    suspend fun actualizarUltimoMensajeConversacion(conversacionId: String, textoResumen: String) {
        runCatching {
            SupaClient.client.from("conversaciones")
                .update({
                    set("ultimo_mensaje", textoResumen)
                }) {
                    filter { eq("id", conversacionId) }
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
     * - Registra todos los callbacks con CoroutineStart.UNDISPATCHED antes de suscribir
     *   (elimina la condicion de carrera en dispositivos independientes).
     * - Mantiene reconexion automatica ante cambios de red (Wi-Fi <-> 4G/5G).
     */
    fun observarSalaRealtime(conversacionId: String, miUsuarioId: String): Flow<ChatRoomRealtimeEvent> = callbackFlow {
        val channelId = "emprende_room_$conversacionId"

        canalesSala.remove(conversacionId)?.let { previo ->
            RealtimeChannelHelper.removerCanalSiCoincide(previo)
        }
        RealtimeChannelHelper.limpiarCanalResidualPorTopic(channelId)

        val channel = SupaClient.client.channel(channelId) {
            broadcast { receiveOwnBroadcasts = false }
            presence { key = miUsuarioId }
        }
        canalesSala[conversacionId] = channel

        val mensajesJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mensajes"
            }.collect { action ->
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
            }
        }

        val conversacionJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "conversaciones"
            }.collect { action ->
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
            }
        }

        val typingJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.broadcastFlow<TypingBroadcastDto>(event = "typing").collect { dto ->
                if (dto.usuarioId != miUsuarioId) {
                    trySend(ChatRoomRealtimeEvent.ContraparteEscribiendo(dto.escribiendo))
                }
            }
        }

        val usuariosPresentes = mutableSetOf<String>()
        val presenceJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.presenceChangeFlow().collect { action ->
                action.decodeJoinsAs<ChatPresenceDto>().forEach { usuariosPresentes.add(it.usuarioId) }
                action.decodeLeavesAs<ChatPresenceDto>().forEach { usuariosPresentes.remove(it.usuarioId) }
                trySend(ChatRoomRealtimeEvent.ContraparteEnLinea(usuariosPresentes.any { it != miUsuarioId }))
            }
        }

        val subscribeJob = launch {
            RealtimeChannelHelper.suscribirConReintentos(channel) { esReconexion ->
                runCatching {
                    channel.track(buildJsonObject { put("usuario_id", miUsuarioId) })
                }
                if (esReconexion) {
                    trySend(ChatRoomRealtimeEvent.Reconectado)
                }
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
                RealtimeChannelHelper.removerCanalSiCoincide(channel)
            }
        }
    }

    /**
     * Observa cambios globales en conversaciones y mensajes para refrescar la bandeja,
     * bloqueos, contadores de no leidos y disparar notificaciones nativas en vivo.
     */
    fun observarBandejaRealtime(
        usuarioId: String,
        miEmprendimientoId: String?
    ): Flow<ChatInboxRealtimeEvent> = callbackFlow {
        val channelId = RealtimeChannelHelper.uniqueChannelId("emprende_inbox_${usuarioId.take(10)}")
        val channel = SupaClient.client.channel(channelId)

        val convJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "conversaciones"
            }.collect { action ->
                val relevante = when (action) {
                    is PostgresAction.Insert -> action.decodeRecordOrNull<ConversacionDto>()?.let {
                        it.compradorId == usuarioId ||
                            (!miEmprendimientoId.isNullOrBlank() && it.emprendimientoId == miEmprendimientoId)
                    } ?: true
                    is PostgresAction.Update -> action.decodeRecordOrNull<ConversacionDto>()?.let {
                        it.compradorId == usuarioId ||
                            (!miEmprendimientoId.isNullOrBlank() && it.emprendimientoId == miEmprendimientoId)
                    } ?: true
                    is PostgresAction.Delete -> true
                    else -> false
                }
                if (relevante) trySend(ChatInboxRealtimeEvent.ConversacionesActualizadas)
            }
        }

        val msgJob = launch(start = CoroutineStart.UNDISPATCHED) {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mensajes"
            }.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val dto = action.decodeRecordOrNull<MensajeDto>()
                        if (dto != null) {
                            trySend(ChatInboxRealtimeEvent.MensajeRecibido(dto.toDomain()))
                        } else {
                            trySend(ChatInboxRealtimeEvent.ConversacionesActualizadas)
                        }
                    }
                    is PostgresAction.Update -> {
                        trySend(ChatInboxRealtimeEvent.ConversacionesActualizadas)
                    }
                    else -> Unit
                }
            }
        }

        val subJob = launch {
            RealtimeChannelHelper.suscribirConReintentos(channel) { esReconexion ->
                if (esReconexion) {
                    trySend(ChatInboxRealtimeEvent.Reconectado)
                }
            }
        }

        awaitClose {
            subJob.cancel()
            convJob.cancel()
            msgJob.cancel()
            CoroutineScope(Dispatchers.IO).launch {
                RealtimeChannelHelper.removerCanalSiCoincide(channel)
            }
        }
    }
}
