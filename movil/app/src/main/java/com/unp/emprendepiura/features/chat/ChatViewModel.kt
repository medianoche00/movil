package com.unp.emprendepiura.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.data.repository.CatalogRepository
import com.unp.emprendepiura.data.repository.ChatRepository
import com.unp.emprendepiura.domain.model.ChatRoomRealtimeEvent
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Mensaje
import com.unp.emprendepiura.domain.model.Usuario
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val loadingConversaciones: Boolean = false,
    val loadingMensajes: Boolean = false,
    val enviando: Boolean = false,
    val procesandoAccionChat: Boolean = false,
    val conversaciones: List<Conversacion> = emptyList(),
    val conversacionActiva: Conversacion? = null,
    val tiendaContraparte: Emprendimiento? = null,
    val compradorContraparte: Usuario? = null,
    val usuariosCache: Map<String, Usuario> = emptyMap(),
    val mensajes: List<Mensaje> = emptyList(),
    val contraparteEnLinea: Boolean = false,
    val contraparteEscribiendo: Boolean = false,
    val salaEliminadaPorContraparte: Boolean = false,
    val error: String? = null,
    val exito: String? = null
) {
    val totalNoLeidos: Int
        get() = conversaciones.sumOf { it.mensajesNoLeidos }
}

class ChatViewModel(
    private val chatRepo: ChatRepository = ChatRepository(),
    private val catalogRepo: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var usuarioObservadoId: String? = null
    private var tiendaObservadaId: String? = null

    private var inboxRealtimeJob: Job? = null
    private var inboxDebounceJob: Job? = null
    private var roomRealtimeJob: Job? = null
    private var localTypingIdleJob: Job? = null
    private var remoteTypingTimeoutJob: Job? = null

    private var transmitiendoEscritura = false
    private var ultimoEnvioTypingMs = 0L

    fun iniciarObservacionBandeja(usuarioId: String, miEmprendimientoId: String?) {
        if (usuarioId.isBlank()) return
        val mismoContexto = usuarioObservadoId == usuarioId &&
            tiendaObservadaId == miEmprendimientoId &&
            inboxRealtimeJob?.isActive == true

        usuarioObservadoId = usuarioId
        tiendaObservadaId = miEmprendimientoId

        cargarConversaciones(usuarioId, miEmprendimientoId, silencioso = mismoContexto)

        if (mismoContexto) return

        inboxRealtimeJob?.cancel()
        inboxRealtimeJob = viewModelScope.launch {
            chatRepo.observarBandejaRealtime(usuarioId, miEmprendimientoId).collect {
                inboxDebounceJob?.cancel()
                inboxDebounceJob = launch {
                    delay(260)
                    cargarConversaciones(usuarioId, miEmprendimientoId, silencioso = true)
                }
            }
        }
    }

    fun cargarConversaciones(
        usuarioId: String,
        miEmprendimientoId: String?,
        silencioso: Boolean = false
    ) {
        if (usuarioId.isBlank()) return
        viewModelScope.launch {
            if (!silencioso) {
                _state.update { it.copy(loadingConversaciones = true, error = null) }
            }
            try {
                val (lista, usuariosMap) = withContext(Dispatchers.IO) {
                    val convs = chatRepo.getConversacionesUsuario(usuarioId, miEmprendimientoId)
                    val tiendas = catalogRepo.getEmprendimientosAprobados().associateBy { it.id }
                    val usuarios = runCatching {
                        SupaClient.client.from("usuarios").select().decodeList<UsuarioRowDto>()
                            .map { it.toDomain() }
                    }.getOrDefault(emptyList()).associateBy { it.id }

                    val enriquecidas = convs.map { c ->
                        val contraparte = if (c.compradorId == usuarioId) {
                            tiendas[c.emprendimientoId]?.nombreTienda ?: "Emprendimiento"
                        } else {
                            usuarios[c.compradorId]?.nombre ?: "Comprador"
                        }
                        c.copy(nombreContraparte = contraparte)
                    }
                    enriquecidas to usuarios
                }
                _state.update { s ->
                    val activaActualizada = s.conversacionActiva?.let { act ->
                        val fresca = lista.firstOrNull { it.id == act.id }
                        fresca?.copy(
                            nombreContraparte = fresca.nombreContraparte.ifBlank { act.nombreContraparte },
                            mensajesNoLeidos = 0
                        ) ?: act
                    }
                    s.copy(
                        loadingConversaciones = false,
                        usuariosCache = usuariosMap,
                        conversaciones = lista.map { c ->
                            if (c.id == activaActualizada?.id) c.copy(mensajesNoLeidos = 0) else c
                        },
                        conversacionActiva = activaActualizada
                    )
                }
            } catch (_: Exception) {
                if (!silencioso) {
                    _state.update { it.copy(loadingConversaciones = false, error = "Error al cargar conversaciones.") }
                }
            }
        }
    }

    fun abrirChatConTienda(
        compradorId: String,
        emprendimientoId: String,
        nombreTienda: String,
        productoId: String? = null,
        ordenId: String? = null,
        onAbierto: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loadingMensajes = true, error = null) }
            try {
                val conv = withContext(Dispatchers.IO) {
                    chatRepo.obtenerOCrearConversacion(compradorId, emprendimientoId, productoId, ordenId)
                }.copy(nombreContraparte = nombreTienda)
                abrirSala(conv, compradorId)
                onAbierto()
            } catch (_: Exception) {
                _state.update { it.copy(loadingMensajes = false, error = "No se pudo iniciar la conversación.") }
            }
        }
    }

    fun abrirChatConComprador(
        miUsuarioId: String,
        compradorId: String,
        emprendimientoId: String,
        nombreComprador: String,
        ordenId: String? = null,
        onAbierto: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loadingMensajes = true, error = null) }
            try {
                val conv = withContext(Dispatchers.IO) {
                    chatRepo.obtenerOCrearConversacion(compradorId, emprendimientoId, null, ordenId)
                }.copy(nombreContraparte = nombreComprador.ifBlank { "Comprador" })
                abrirSala(conv, miUsuarioId)
                onAbierto()
            } catch (_: Exception) {
                _state.update { it.copy(loadingMensajes = false, error = "No se pudo abrir el chat con el comprador.") }
            }
        }
    }

    fun abrirSala(conversacion: Conversacion, miUsuarioId: String) {
        roomRealtimeJob?.cancel()
        localTypingIdleJob?.cancel()
        remoteTypingTimeoutJob?.cancel()
        transmitiendoEscritura = false

        _state.update { s ->
            s.copy(
                conversacionActiva = conversacion.copy(mensajesNoLeidos = 0),
                conversaciones = s.conversaciones.map {
                    if (it.id == conversacion.id) it.copy(mensajesNoLeidos = 0) else it
                },
                mensajes = emptyList(),
                loadingMensajes = true,
                tiendaContraparte = null,
                compradorContraparte = s.usuariosCache[conversacion.compradorId],
                contraparteEnLinea = false,
                contraparteEscribiendo = false,
                salaEliminadaPorContraparte = false,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val msgs = withContext(Dispatchers.IO) {
                    chatRepo.getMensajes(conversacion.id)
                }
                val tienda = withContext(Dispatchers.IO) {
                    catalogRepo.getEmprendimientoPorId(conversacion.emprendimientoId)
                }
                val comprador = withContext(Dispatchers.IO) {
                    _state.value.usuariosCache[conversacion.compradorId] ?: runCatching {
                        SupaClient.client.from("usuarios")
                            .select { filter { eq("id", conversacion.compradorId) } }
                            .decodeList<UsuarioRowDto>()
                            .firstOrNull()
                            ?.toDomain()
                    }.getOrNull()
                }
                _state.update {
                    it.copy(
                        loadingMensajes = false,
                        mensajes = msgs,
                        tiendaContraparte = tienda,
                        compradorContraparte = comprador
                    )
                }
                // Marcar como leidos los mensajes recibidos al entrar a la sala
                if (msgs.any { it.remitenteId != miUsuarioId && !it.leido }) {
                    withContext(Dispatchers.IO) {
                        chatRepo.marcarMensajesComoLeidos(conversacion.id, miUsuarioId)
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(loadingMensajes = false, error = "No se pudieron cargar los mensajes.") }
            }
        }

        roomRealtimeJob = viewModelScope.launch {
            chatRepo.observarSalaRealtime(conversacion.id, miUsuarioId).collect { evento ->
                when (evento) {
                    is ChatRoomRealtimeEvent.NuevoMensaje -> {
                        val nuevo = evento.mensaje
                        _state.update { s ->
                            val listaMsgs = if (s.mensajes.any { it.id == nuevo.id }) s.mensajes else s.mensajes + nuevo
                            val activa = s.conversacionActiva?.copy(
                                ultimoMensaje = nuevo.contenido,
                                actualizadoEn = nuevo.creadoEn
                            )
                            s.copy(
                                mensajes = listaMsgs,
                                conversacionActiva = activa,
                                contraparteEscribiendo = if (nuevo.remitenteId != miUsuarioId) false else s.contraparteEscribiendo
                            )
                        }
                        if (nuevo.remitenteId != miUsuarioId && !nuevo.leido) {
                            launch(Dispatchers.IO) {
                                chatRepo.marcarMensajesComoLeidos(conversacion.id, miUsuarioId)
                            }
                        }
                    }

                    is ChatRoomRealtimeEvent.MensajeActualizado -> {
                        val actualizado = evento.mensaje
                        _state.update { s ->
                            s.copy(
                                mensajes = s.mensajes.map {
                                    if (it.id == actualizado.id) actualizado else it
                                }
                            )
                        }
                    }

                    is ChatRoomRealtimeEvent.ConversacionActualizada -> {
                        val act = evento.conversacion
                        _state.update { s ->
                            val nuevaActiva = s.conversacionActiva?.let { prev ->
                                if (prev.id == act.id) {
                                    prev.copy(
                                        ultimoMensaje = act.ultimoMensaje.ifBlank { prev.ultimoMensaje },
                                        actualizadoEn = act.actualizadoEn.ifBlank { prev.actualizadoEn },
                                        bloqueadoPor = act.bloqueadoPor,
                                        bloqueadoEn = act.bloqueadoEn
                                    )
                                } else prev
                            }
                            val nuevasConvs = s.conversaciones.map { c ->
                                if (c.id == act.id) {
                                    c.copy(
                                        ultimoMensaje = act.ultimoMensaje.ifBlank { c.ultimoMensaje },
                                        actualizadoEn = act.actualizadoEn.ifBlank { c.actualizadoEn },
                                        bloqueadoPor = act.bloqueadoPor,
                                        bloqueadoEn = act.bloqueadoEn
                                    )
                                } else c
                            }
                            s.copy(
                                conversacionActiva = nuevaActiva,
                                conversaciones = nuevasConvs,
                                contraparteEscribiendo = if (act.estaBloqueada) false else s.contraparteEscribiendo
                            )
                        }
                    }

                    is ChatRoomRealtimeEvent.ConversacionEliminada -> {
                        _state.update { s ->
                            s.copy(
                                conversaciones = s.conversaciones.filterNot { it.id == evento.conversacionId },
                                conversacionActiva = null,
                                mensajes = emptyList(),
                                contraparteEnLinea = false,
                                contraparteEscribiendo = false,
                                salaEliminadaPorContraparte = true,
                                error = "La conversación fue eliminada."
                            )
                        }
                    }

                    is ChatRoomRealtimeEvent.ContraparteEscribiendo -> {
                        remoteTypingTimeoutJob?.cancel()
                        _state.update { it.copy(contraparteEscribiendo = evento.escribiendo) }
                        if (evento.escribiendo) {
                            remoteTypingTimeoutJob = launch {
                                delay(4500)
                                _state.update { it.copy(contraparteEscribiendo = false) }
                            }
                        }
                    }

                    is ChatRoomRealtimeEvent.ContraparteEnLinea -> {
                        _state.update {
                            it.copy(
                                contraparteEnLinea = evento.enLinea,
                                contraparteEscribiendo = if (evento.enLinea) it.contraparteEscribiendo else false
                            )
                        }
                    }
                }
            }
        }
    }

    fun notificarEscritura(miUsuarioId: String, escribiendo: Boolean) {
        val conv = _state.value.conversacionActiva ?: return
        if (conv.estaBloqueada) return

        val ahora = System.currentTimeMillis()
        if (escribiendo) {
            if (!transmitiendoEscritura || (ahora - ultimoEnvioTypingMs) > 1800L) {
                transmitiendoEscritura = true
                ultimoEnvioTypingMs = ahora
                viewModelScope.launch(Dispatchers.IO) {
                    chatRepo.enviarIndicadorEscritura(conv.id, miUsuarioId, true)
                }
            }
            localTypingIdleJob?.cancel()
            localTypingIdleJob = viewModelScope.launch {
                delay(2400)
                if (transmitiendoEscritura) {
                    transmitiendoEscritura = false
                    withContext(Dispatchers.IO) {
                        chatRepo.enviarIndicadorEscritura(conv.id, miUsuarioId, false)
                    }
                }
            }
        } else {
            localTypingIdleJob?.cancel()
            if (transmitiendoEscritura) {
                transmitiendoEscritura = false
                viewModelScope.launch(Dispatchers.IO) {
                    chatRepo.enviarIndicadorEscritura(conv.id, miUsuarioId, false)
                }
            }
        }
    }

    fun enviarMensaje(remitenteId: String, texto: String) {
        val conv = _state.value.conversacionActiva ?: return
        if (conv.estaBloqueada) {
            val aviso = if (conv.bloqueadaPorMi(remitenteId)) {
                "Bloqueaste esta conversación. Desbloquéala para volver a escribir."
            } else {
                "Esta conversación fue bloqueada por la contraparte."
            }
            _state.update { it.copy(error = aviso) }
            return
        }

        val clean = texto.trim()
        if (clean.isEmpty()) return

        notificarEscritura(remitenteId, false)

        viewModelScope.launch {
            _state.update { it.copy(enviando = true, error = null) }
            try {
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, clean)
                }
                _state.update { s ->
                    val actualizados = if (s.mensajes.any { it.id == enviado.id }) s.mensajes else s.mensajes + enviado
                    s.copy(
                        enviando = false,
                        mensajes = actualizados,
                        conversacionActiva = s.conversacionActiva?.copy(
                            ultimoMensaje = enviado.contenido,
                            actualizadoEn = enviado.creadoEn
                        )
                    )
                }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("bloqueada", ignoreCase = true) == true) {
                    "Esta conversación se encuentra bloqueada."
                } else {
                    "No se pudo enviar el mensaje."
                }
                _state.update { it.copy(enviando = false, error = msg) }
            }
        }
    }

    fun bloquearConversacion(conversacionId: String, miUsuarioId: String) {
        if (miUsuarioId.isBlank()) return
        notificarEscritura(miUsuarioId, false)

        viewModelScope.launch {
            _state.update { it.copy(procesandoAccionChat = true, error = null) }
            try {
                val actualizada = withContext(Dispatchers.IO) {
                    chatRepo.actualizarBloqueoConversacion(conversacionId, miUsuarioId)
                }
                aplicarBloqueoEnEstado(actualizada, "Conversación bloqueada. Ninguna parte podrá enviar mensajes.")
            } catch (e: Exception) {
                val detalle = if (e.message?.contains("bloqueado_por", ignoreCase = true) == true) {
                    "Ejecuta el script 03_alter_chat_realtime_bloqueos.sql en Supabase para guardar bloqueos."
                } else {
                    "No se pudo bloquear la conversación."
                }
                _state.update { it.copy(procesandoAccionChat = false, error = detalle) }
            }
        }
    }

    fun desbloquearConversacion(conversacionId: String, miUsuarioId: String) {
        val objetivo = _state.value.conversacionActiva?.takeIf { it.id == conversacionId }
            ?: _state.value.conversaciones.firstOrNull { it.id == conversacionId }

        if (objetivo != null && objetivo.estaBloqueada && !objetivo.bloqueadaPorMi(miUsuarioId)) {
            _state.update { it.copy(error = "Solo quien bloqueó la conversación puede desbloquearla.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(procesandoAccionChat = true, error = null) }
            try {
                val actualizada = withContext(Dispatchers.IO) {
                    chatRepo.actualizarBloqueoConversacion(conversacionId, null)
                }
                aplicarBloqueoEnEstado(actualizada, "Conversación desbloqueada.")
            } catch (_: Exception) {
                _state.update { it.copy(procesandoAccionChat = false, error = "No se pudo desbloquear la conversación.") }
            }
        }
    }

    private fun aplicarBloqueoEnEstado(actualizada: Conversacion, mensajeExito: String) {
        _state.update { s ->
            val nuevaActiva = s.conversacionActiva?.let { act ->
                if (act.id == actualizada.id) {
                    act.copy(
                        bloqueadoPor = actualizada.bloqueadoPor,
                        bloqueadoEn = actualizada.bloqueadoEn
                    )
                } else act
            }
            val nuevasConvs = s.conversaciones.map { c ->
                if (c.id == actualizada.id) {
                    c.copy(
                        bloqueadoPor = actualizada.bloqueadoPor,
                        bloqueadoEn = actualizada.bloqueadoEn
                    )
                } else c
            }
            s.copy(
                procesandoAccionChat = false,
                conversacionActiva = nuevaActiva,
                conversaciones = nuevasConvs,
                contraparteEscribiendo = if (actualizada.estaBloqueada) false else s.contraparteEscribiendo,
                exito = mensajeExito
            )
        }
    }

    fun eliminarConversacion(conversacionId: String, onEliminado: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(procesandoAccionChat = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    chatRepo.eliminarConversacion(conversacionId)
                }
                _state.update { s ->
                    val filtradas = s.conversaciones.filterNot { it.id == conversacionId }
                    val activa = if (s.conversacionActiva?.id == conversacionId) null else s.conversacionActiva
                    s.copy(
                        procesandoAccionChat = false,
                        conversaciones = filtradas,
                        conversacionActiva = activa,
                        mensajes = if (activa == null) emptyList() else s.mensajes,
                        exito = "Conversación eliminada definitivamente."
                    )
                }
                onEliminado()
            } catch (_: Exception) {
                _state.update { it.copy(procesandoAccionChat = false, error = "No se pudo eliminar la conversación.") }
            }
        }
    }

    fun cerrarSala(miUsuarioId: String? = null) {
        val convId = _state.value.conversacionActiva?.id
        if (!miUsuarioId.isNullOrBlank() && !convId.isNullOrBlank() && transmitiendoEscritura) {
            transmitiendoEscritura = false
            viewModelScope.launch(Dispatchers.IO) {
                chatRepo.enviarIndicadorEscritura(convId, miUsuarioId, false)
            }
        }
        localTypingIdleJob?.cancel()
        localTypingIdleJob = null
        remoteTypingTimeoutJob?.cancel()
        remoteTypingTimeoutJob = null
        roomRealtimeJob?.cancel()
        roomRealtimeJob = null
        _state.update {
            it.copy(
                conversacionActiva = null,
                mensajes = emptyList(),
                tiendaContraparte = null,
                compradorContraparte = null,
                contraparteEnLinea = false,
                contraparteEscribiendo = false,
                salaEliminadaPorContraparte = false
            )
        }
    }

    fun detenerObservacionYSesion() {
        cerrarSala()
        inboxDebounceJob?.cancel()
        inboxRealtimeJob?.cancel()
        inboxRealtimeJob = null
        usuarioObservadoId = null
        tiendaObservadaId = null
        _state.value = ChatUiState()
    }

    fun consumirEventoSalaEliminada() {
        _state.update { it.copy(salaEliminadaPorContraparte = false) }
    }

    fun limpiarMensajes() {
        _state.update { it.copy(error = null, exito = null) }
    }

    override fun onCleared() {
        super.onCleared()
        roomRealtimeJob?.cancel()
        inboxRealtimeJob?.cancel()
        inboxDebounceJob?.cancel()
        localTypingIdleJob?.cancel()
        remoteTypingTimeoutJob?.cancel()
    }
}
