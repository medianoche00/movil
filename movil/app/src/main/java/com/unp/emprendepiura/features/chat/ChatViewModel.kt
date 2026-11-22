package com.unp.emprendepiura.features.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.network.RealtimeChannelHelper
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.core.notifications.EmprendeNotificationHelper
import com.unp.emprendepiura.core.util.UriBytesUtil
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.data.repository.CatalogRepository
import com.unp.emprendepiura.data.repository.ChatRepository
import com.unp.emprendepiura.domain.model.ChatInboxRealtimeEvent
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Archivo: ChatViewModel.kt
 * Proposito: Estado y orquestacion de mensajeria en vivo, bloqueos bidireccionales con
 *            privacidad discreta, red de respaldo anti-desconexion y notificaciones nativas.
 */
data class ChatUiState(
    val loadingConversaciones: Boolean = false,
    val loadingMensajes: Boolean = false,
    val enviando: Boolean = false,
    val enviandoAdjunto: Boolean = false,
    val etiquetaAdjuntoEnProgreso: String? = null,
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

    /**
     * Emprendimientos con los que el usuario (en rol comprador) mantiene un bloqueo activo
     * en cualquier direccion. Sirve para ocultarlos del catalogo y carrito al instante.
     */
    fun tiendasBloqueadasParaComprador(miUsuarioId: String): Set<String> {
        if (miUsuarioId.isBlank()) return emptySet()
        val desdeLista = conversaciones.asSequence()
            .filter { it.compradorId == miUsuarioId && it.estaBloqueada }
            .map { it.emprendimientoId }
        val desdeActiva = conversacionActiva
            ?.takeIf { it.compradorId == miUsuarioId && it.estaBloqueada }
            ?.emprendimientoId
        return (if (desdeActiva != null) desdeLista + desdeActiva else desdeLista).toSet()
    }
}

class ChatViewModel(
    private val chatRepo: ChatRepository = ChatRepository(),
    private val catalogRepo: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var usuarioObservadoId: String? = null
    private var tiendaObservadaId: String? = null
    private var appEnPrimerPlano: Boolean = true
    private var cargaInicialBandejaLista: Boolean = false

    private var inboxRealtimeJob: Job? = null
    private var inboxDebounceJob: Job? = null
    private var inboxFallbackJob: Job? = null
    private var roomRealtimeJob: Job? = null
    private var roomFallbackJob: Job? = null
    private var localTypingIdleJob: Job? = null
    private var remoteTypingTimeoutJob: Job? = null

    private var transmitiendoEscritura = false
    private var ultimoEnvioTypingMs = 0L

    private val mensajesNotificados = LinkedHashSet<String>()
    private val ultimoAvisoBandejaPorConv = mutableMapOf<String, String>()

    fun actualizarAppEnPrimerPlano(enPrimerPlano: Boolean) {
        appEnPrimerPlano = enPrimerPlano
        if (enPrimerPlano) {
            val uid = usuarioObservadoId
            val convActiva = _state.value.conversacionActiva
            if (!uid.isNullOrBlank() && convActiva != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    chatRepo.marcarMensajesComoLeidos(convActiva.id, uid)
                }
            }
        }
    }

    fun iniciarObservacionBandeja(
        context: Context,
        usuarioId: String,
        miEmprendimientoId: String?
    ) {
        if (usuarioId.isBlank()) return
        appContext = context.applicationContext

        val mismoContexto = usuarioObservadoId == usuarioId &&
            tiendaObservadaId == miEmprendimientoId &&
            inboxRealtimeJob?.isActive == true

        val cambioUsuario = usuarioObservadoId != usuarioId
        if (cambioUsuario) {
            cargaInicialBandejaLista = false
            mensajesNotificados.clear()
            ultimoAvisoBandejaPorConv.clear()
        }

        usuarioObservadoId = usuarioId
        tiendaObservadaId = miEmprendimientoId

        cargarConversaciones(usuarioId, miEmprendimientoId, silencioso = mismoContexto)

        if (mismoContexto) return

        inboxRealtimeJob?.cancel()
        inboxRealtimeJob = viewModelScope.launch {
            chatRepo.observarBandejaRealtime(usuarioId, miEmprendimientoId).collect { evento ->
                when (evento) {
                    is ChatInboxRealtimeEvent.MensajeRecibido -> {
                        procesarNotificacionMensajeEntrante(evento.mensaje, usuarioId, miEmprendimientoId)
                        programarRefrescoBandeja(usuarioId, miEmprendimientoId)
                    }
                    ChatInboxRealtimeEvent.ConversacionesActualizadas,
                    ChatInboxRealtimeEvent.Reconectado -> {
                        programarRefrescoBandeja(usuarioId, miEmprendimientoId)
                    }
                }
            }
        }

        // Red de respaldo: garantiza sincronizacion incluso bajo redes Wi-Fi restrictivas
        inboxFallbackJob?.cancel()
        inboxFallbackJob = viewModelScope.launch {
            while (isActive) {
                val esperaMs = if (RealtimeChannelHelper.estaConectado()) 16_000L else 5_500L
                delay(esperaMs)
                cargarConversaciones(
                    usuarioId = usuarioId,
                    miEmprendimientoId = miEmprendimientoId,
                    silencioso = true,
                    detectarNuevosParaNotificar = true
                )
            }
        }
    }

    private fun programarRefrescoBandeja(usuarioId: String, miEmprendimientoId: String?) {
        inboxDebounceJob?.cancel()
        inboxDebounceJob = viewModelScope.launch {
            delay(220)
            cargarConversaciones(usuarioId, miEmprendimientoId, silencioso = true)
        }
    }

    private fun procesarNotificacionMensajeEntrante(
        mensaje: Mensaje,
        miUsuarioId: String,
        miEmprendimientoId: String?
    ) {
        if (mensaje.remitenteId == miUsuarioId) return
        val claveMsg = mensaje.id.ifBlank { "${mensaje.conversacionId}_${mensaje.creadoEn}_${mensaje.contenido}" }
        if (!registrarClaveNotificada(claveMsg)) return

        val viendoEstaSalaEnPrimerPlano = appEnPrimerPlano &&
            _state.value.conversacionActiva?.id == mensaje.conversacionId
        if (viendoEstaSalaEnPrimerPlano) return

        val ctx = appContext ?: return

        viewModelScope.launch {
            val convLocal = _state.value.conversaciones.firstOrNull { it.id == mensaje.conversacionId }
            val convVerificada = convLocal ?: withContext(Dispatchers.IO) {
                runCatching { chatRepo.getConversacionPorId(mensaje.conversacionId) }.getOrNull()
            } ?: return@launch

            val perteneceAMi = convVerificada.compradorId == miUsuarioId ||
                (!miEmprendimientoId.isNullOrBlank() && convVerificada.emprendimientoId == miEmprendimientoId)
            if (!perteneceAMi) return@launch

            val nombreRemitente = convVerificada.nombreContraparte.ifBlank {
                if (convVerificada.compradorId == miUsuarioId) {
                    withContext(Dispatchers.IO) {
                        runCatching { catalogRepo.getEmprendimientoPorId(convVerificada.emprendimientoId)?.nombreTienda }
                            .getOrNull()
                    } ?: "Emprendimiento"
                } else {
                    _state.value.usuariosCache[convVerificada.compradorId]?.nombre ?: "Comprador"
                }
            }

            ultimoAvisoBandejaPorConv[convVerificada.id] = mensaje.creadoEn
            EmprendeNotificationHelper.mostrarNotificacionMensaje(
                context = ctx,
                conversacionId = convVerificada.id,
                nombreRemitente = nombreRemitente,
                contenido = ChatMessageCodec.resumenLegible(mensaje.contenido)
            )
        }
    }

    private fun registrarClaveNotificada(clave: String): Boolean {
        if (clave in mensajesNotificados) return false
        mensajesNotificados.add(clave)
        if (mensajesNotificados.size > 200) {
            val primero = mensajesNotificados.firstOrNull()
            if (primero != null) mensajesNotificados.remove(primero)
        }
        return true
    }

    fun cargarConversaciones(
        usuarioId: String,
        miEmprendimientoId: String?,
        silencioso: Boolean = false,
        detectarNuevosParaNotificar: Boolean = false
    ) {
        if (usuarioId.isBlank()) return
        viewModelScope.launch {
            if (!silencioso) {
                _state.update { it.copy(loadingConversaciones = true, error = null) }
            }
            try {
                val prevPorId = _state.value.conversaciones.associateBy { it.id }
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

                // Si llego un mensaje por respaldo (cuando el WebSocket estuvo bloqueado), notificar
                if (detectarNuevosParaNotificar && cargaInicialBandejaLista) {
                    val ctx = appContext
                    val activaId = _state.value.conversacionActiva?.id
                    if (ctx != null) {
                        lista.forEach { nueva ->
                            val previa = prevPorId[nueva.id]
                            val aumentoNoLeidos = nueva.mensajesNoLeidos > (previa?.mensajesNoLeidos ?: 0)
                            val cambioTimestamp = nueva.actualizadoEn.isNotBlank() &&
                                nueva.actualizadoEn != previa?.actualizadoEn &&
                                nueva.actualizadoEn != ultimoAvisoBandejaPorConv[nueva.id]
                            val enSalaVisible = appEnPrimerPlano && activaId == nueva.id

                            if (aumentoNoLeidos && cambioTimestamp && !enSalaVisible && nueva.ultimoMensaje.isNotBlank()) {
                                ultimoAvisoBandejaPorConv[nueva.id] = nueva.actualizadoEn
                                EmprendeNotificationHelper.mostrarNotificacionMensaje(
                                    context = ctx,
                                    conversacionId = nueva.id,
                                    nombreRemitente = nueva.nombreContraparte,
                                    contenido = ChatMessageCodec.resumenLegible(nueva.ultimoMensaje)
                                )
                            }
                        }
                    }
                }
                cargaInicialBandejaLista = true

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
                    _state.update { it.copy(loadingConversaciones = false, error = "Error al cargar conversaciones.", exito = null) }
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
        roomFallbackJob?.cancel()
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
                        .filterNot { ChatMessageCodec.estaOcultoPara(it.contenido, miUsuarioId) }
                }
                val convFresca = withContext(Dispatchers.IO) {
                    runCatching { chatRepo.getConversacionPorId(conversacion.id) }.getOrNull()
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
                _state.update { s ->
                    val activa = s.conversacionActiva?.let { act ->
                        if (convFresca != null && act.id == convFresca.id) {
                            act.copy(
                                bloqueadoPor = convFresca.bloqueadoPor,
                                bloqueadoEn = convFresca.bloqueadoEn,
                                ultimoMensaje = convFresca.ultimoMensaje.ifBlank { act.ultimoMensaje },
                                actualizadoEn = convFresca.actualizadoEn.ifBlank { act.actualizadoEn }
                            )
                        } else act
                    }
                    s.copy(
                        loadingMensajes = false,
                        mensajes = msgs,
                        conversacionActiva = activa,
                        tiendaContraparte = tienda,
                        compradorContraparte = comprador
                    )
                }
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
                        if (!ChatMessageCodec.estaOcultoPara(nuevo.contenido, miUsuarioId)) {
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
                            if (nuevo.remitenteId != miUsuarioId && !nuevo.leido && appEnPrimerPlano) {
                                launch(Dispatchers.IO) {
                                    chatRepo.marcarMensajesComoLeidos(conversacion.id, miUsuarioId)
                                }
                            }
                        }
                    }

                    is ChatRoomRealtimeEvent.MensajeActualizado -> {
                        val actualizado = evento.mensaje
                        val ocultoParaMi = ChatMessageCodec.estaOcultoPara(actualizado.contenido, miUsuarioId)
                        _state.update { s ->
                            val nuevaLista = if (ocultoParaMi) {
                                s.mensajes.filterNot { it.id == actualizado.id }
                            } else {
                                s.mensajes.map { if (it.id == actualizado.id) actualizado else it }
                            }
                            s.copy(mensajes = nuevaLista)
                        }
                    }

                    is ChatRoomRealtimeEvent.ConversacionActualizada -> {
                        aplicarActualizacionSala(evento.conversacion)
                    }

                    is ChatRoomRealtimeEvent.ConversacionEliminada -> {
                        marcarSalaEliminada(evento.conversacionId)
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

                    ChatRoomRealtimeEvent.Reconectado -> {
                        sincronizarSalaSilenciosa(conversacion.id, miUsuarioId)
                    }
                }
            }
        }

        // Red de respaldo en sala: mantiene mensajes, visto azul y bloqueos al dia
        // incluso si una red Wi-Fi / operadora limita WebSockets temporalmente.
        roomFallbackJob = viewModelScope.launch {
            while (isActive) {
                val esperaMs = if (RealtimeChannelHelper.estaConectado()) 8_500L else 2_800L
                delay(esperaMs)
                sincronizarSalaSilenciosa(conversacion.id, miUsuarioId)
            }
        }
    }

    private suspend fun sincronizarSalaSilenciosa(conversacionId: String, miUsuarioId: String) {
        runCatching {
            val (convFresca, msgsFrescos) = withContext(Dispatchers.IO) {
                val c = chatRepo.getConversacionPorId(conversacionId)
                val m = if (c != null) {
                    chatRepo.getMensajes(conversacionId)
                        .filterNot { ChatMessageCodec.estaOcultoPara(it.contenido, miUsuarioId) }
                } else emptyList()
                c to m
            }

            if (convFresca == null) {
                marcarSalaEliminada(conversacionId)
                return
            }

            aplicarActualizacionSala(convFresca)
            _state.update { s ->
                if (s.conversacionActiva?.id != conversacionId) s
                else s.copy(mensajes = msgsFrescos)
            }

            if (appEnPrimerPlano && msgsFrescos.any { it.remitenteId != miUsuarioId && !it.leido }) {
                withContext(Dispatchers.IO) {
                    chatRepo.marcarMensajesComoLeidos(conversacionId, miUsuarioId)
                }
            }
        }
    }

    private fun aplicarActualizacionSala(act: Conversacion) {
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

    private fun marcarSalaEliminada(conversacionId: String) {
        _state.update { s ->
            s.copy(
                conversaciones = s.conversaciones.filterNot { it.id == conversacionId },
                conversacionActiva = null,
                mensajes = emptyList(),
                contraparteEnLinea = false,
                contraparteEscribiendo = false,
                salaEliminadaPorContraparte = true,
                error = "La conversación fue eliminada.",
                exito = null
            )
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

    fun enviarMensaje(
        remitenteId: String,
        texto: String,
        replyTo: ChatReplyQuote? = null
    ) {
        val conv = _state.value.conversacionActiva ?: return
        if (!validarSalaDisponibleParaEnvio(conv, remitenteId)) return

        val clean = texto.trim()
        if (clean.isEmpty()) return

        val contenidoFinal = ChatMessageCodec.encode(
            ChatRichPayload(
                type = ChatMediaTipo.TEXT.name,
                text = clean,
                reply = replyTo
            )
        )
        notificarEscritura(remitenteId, false)

        viewModelScope.launch {
            _state.update { it.copy(enviando = true, error = null, exito = null) }
            try {
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, contenidoFinal)
                }
                agregarMensajeEnviadoAEstado(enviado)
            } catch (e: Exception) {
                manejarErrorEnvio(e, conv, remitenteId)
            }
        }
    }

    fun enviarImagenAdjunta(
        context: Context,
        remitenteId: String,
        imageUri: Uri,
        replyTo: ChatReplyQuote? = null
    ) {
        val conv = _state.value.conversacionActiva ?: return
        if (!validarSalaDisponibleParaEnvio(conv, remitenteId)) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    enviandoAdjunto = true,
                    etiquetaAdjuntoEnProgreso = "Enviando imagen...",
                    error = null,
                    exito = null
                )
            }
            try {
                val bytes = withContext(Dispatchers.IO) {
                    UriBytesUtil.comprimirImagenUri(context, imageUri, maxSide = 1400, quality = 82)
                } ?: throw IllegalStateException("No se pudo procesar la imagen seleccionada.")

                val url = withContext(Dispatchers.IO) {
                    chatRepo.subirAdjuntoChat(conv.id, bytes, "jpg", "fotos")
                }
                val contenido = ChatMessageCodec.encode(
                    ChatRichPayload(
                        type = ChatMediaTipo.IMAGE.name,
                        url = url,
                        mimeType = "image/jpeg",
                        fileSizeBytes = bytes.size.toLong(),
                        reply = replyTo
                    )
                )
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, contenido)
                }
                agregarMensajeEnviadoAEstado(enviado)
            } catch (e: Exception) {
                manejarErrorEnvio(e, conv, remitenteId, "No se pudo enviar la imagen.")
            }
        }
    }

    fun enviarAudioAdjunto(
        remitenteId: String,
        audioBytes: ByteArray,
        duracionSeg: Int,
        replyTo: ChatReplyQuote? = null
    ) {
        val conv = _state.value.conversacionActiva ?: return
        if (!validarSalaDisponibleParaEnvio(conv, remitenteId)) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    enviandoAdjunto = true,
                    etiquetaAdjuntoEnProgreso = "Enviando nota de voz...",
                    error = null,
                    exito = null
                )
            }
            try {
                val url = withContext(Dispatchers.IO) {
                    chatRepo.subirAdjuntoChat(conv.id, audioBytes, "m4a", "audios")
                }
                val contenido = ChatMessageCodec.encode(
                    ChatRichPayload(
                        type = ChatMediaTipo.AUDIO.name,
                        url = url,
                        mimeType = "audio/mp4",
                        fileSizeBytes = audioBytes.size.toLong(),
                        durationSec = duracionSeg.coerceAtLeast(1),
                        reply = replyTo
                    )
                )
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, contenido)
                }
                agregarMensajeEnviadoAEstado(enviado)
            } catch (e: Exception) {
                manejarErrorEnvio(e, conv, remitenteId, "No se pudo enviar la nota de voz.")
            }
        }
    }

    fun enviarDocumentoAdjunto(
        context: Context,
        remitenteId: String,
        docUri: Uri,
        replyTo: ChatReplyQuote? = null
    ) {
        val conv = _state.value.conversacionActiva ?: return
        if (!validarSalaDisponibleParaEnvio(conv, remitenteId)) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    enviandoAdjunto = true,
                    etiquetaAdjuntoEnProgreso = "Subiendo documento...",
                    error = null,
                    exito = null
                )
            }
            try {
                val doc = withContext(Dispatchers.IO) {
                    ChatDeviceHelper.leerDocumentoDesdeUri(context, docUri).getOrThrow()
                }
                val url = withContext(Dispatchers.IO) {
                    chatRepo.subirAdjuntoChat(conv.id, doc.bytes, doc.extension, "documentos")
                }
                val contenido = ChatMessageCodec.encode(
                    ChatRichPayload(
                        type = ChatMediaTipo.DOCUMENT.name,
                        url = url,
                        fileName = doc.nombre,
                        mimeType = doc.mimeType,
                        fileSizeBytes = doc.bytes.size.toLong(),
                        reply = replyTo
                    )
                )
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, contenido)
                }
                agregarMensajeEnviadoAEstado(enviado)
            } catch (e: Exception) {
                val fallback = e.message?.takeIf { it.contains("15 MB") || it.contains("vacío") }
                    ?: "No se pudo enviar el documento."
                manejarErrorEnvio(e, conv, remitenteId, fallback)
            }
        }
    }

    fun enviarUbicacionAdjunta(
        context: Context,
        remitenteId: String,
        replyTo: ChatReplyQuote? = null
    ) {
        val conv = _state.value.conversacionActiva ?: return
        if (!validarSalaDisponibleParaEnvio(conv, remitenteId)) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    enviandoAdjunto = true,
                    etiquetaAdjuntoEnProgreso = "Obteniendo ubicación GPS...",
                    error = null,
                    exito = null
                )
            }
            try {
                val ubicacion = ChatDeviceHelper.obtenerUbicacionActual(context)
                    ?: throw IllegalStateException("Activa el GPS del dispositivo e intenta nuevamente.")

                val contenido = ChatMessageCodec.encode(
                    ChatRichPayload(
                        type = ChatMediaTipo.LOCATION.name,
                        lat = ubicacion.lat,
                        lng = ubicacion.lng,
                        addressLabel = ubicacion.direccionLegible,
                        reply = replyTo
                    )
                )
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, contenido)
                }
                agregarMensajeEnviadoAEstado(enviado)
            } catch (e: Exception) {
                manejarErrorEnvio(
                    e,
                    conv,
                    remitenteId,
                    e.message ?: "No se pudo obtener tu ubicación actual."
                )
            }
        }
    }

    fun enviarContactoAdjunto(
        remitenteId: String,
        nombreContacto: String,
        telefonoContacto: String,
        replyTo: ChatReplyQuote? = null
    ) {
        val conv = _state.value.conversacionActiva ?: return
        if (!validarSalaDisponibleParaEnvio(conv, remitenteId)) return

        viewModelScope.launch {
            _state.update { it.copy(enviando = true, error = null, exito = null) }
            try {
                val contenido = ChatMessageCodec.encode(
                    ChatRichPayload(
                        type = ChatMediaTipo.CONTACT.name,
                        contactName = nombreContacto.trim(),
                        contactPhone = telefonoContacto.trim(),
                        reply = replyTo
                    )
                )
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(conv.id, remitenteId, contenido)
                }
                agregarMensajeEnviadoAEstado(enviado)
            } catch (e: Exception) {
                manejarErrorEnvio(e, conv, remitenteId, "No se pudo compartir el contacto.")
            }
        }
    }

    fun mostrarAvisoChat(mensaje: String) {
        _state.update { it.copy(error = mensaje, exito = null) }
    }

    /**
     * Oculta un mensaje unicamente para el usuario actual sin afectar a la contraparte.
     */
    fun eliminarMensajeParaMi(mensaje: Mensaje, miUsuarioId: String) {
        if (mensaje.id.isBlank() || miUsuarioId.isBlank()) return
        val payloadActual = ChatMessageCodec.decode(mensaje.contenido)
        val nuevosOcultos = (payloadActual.hiddenFor + miUsuarioId).distinct()
        val contenidoActualizado = ChatMessageCodec.encode(payloadActual.copy(hiddenFor = nuevosOcultos))

        _state.update { s ->
            s.copy(mensajes = s.mensajes.filterNot { it.id == mensaje.id })
        }

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    chatRepo.actualizarContenidoMensaje(mensaje.id, contenidoActualizado)
                }
            }.onFailure {
                _state.update { it.copy(error = "No se pudo eliminar el mensaje.", exito = null) }
            }
        }
    }

    /**
     * Elimina un mensaje propio para ambas partes en tiempo real y actualiza la vista previa
     * de la conversacion si era el ultimo mensaje enviado.
     */
    fun eliminarMensajeParaTodos(mensaje: Mensaje, miUsuarioId: String) {
        if (mensaje.id.isBlank() || miUsuarioId.isBlank()) return
        if (mensaje.remitenteId != miUsuarioId) {
            _state.update { it.copy(error = "Solo puedes eliminar para todos tus propios mensajes.", exito = null) }
            return
        }

        val payloadActual = ChatMessageCodec.decode(mensaje.contenido)
        val contenidoEliminado = ChatMessageCodec.encode(
            ChatRichPayload(
                type = ChatMediaTipo.DELETED.name,
                hiddenFor = payloadActual.hiddenFor
            )
        )
        val eraUltimoMensaje = _state.value.mensajes.lastOrNull()?.id == mensaje.id

        viewModelScope.launch {
            try {
                val actualizado = withContext(Dispatchers.IO) {
                    chatRepo.actualizarContenidoMensaje(mensaje.id, contenidoEliminado)
                }
                if (eraUltimoMensaje) {
                    withContext(Dispatchers.IO) {
                        chatRepo.actualizarUltimoMensajeConversacion(
                            mensaje.conversacionId,
                            "Se eliminó este mensaje"
                        )
                    }
                }
                _state.update { s ->
                    s.copy(
                        mensajes = s.mensajes.map { if (it.id == actualizado.id) actualizado else it },
                        conversacionActiva = if (eraUltimoMensaje) {
                            s.conversacionActiva?.copy(ultimoMensaje = "Se eliminó este mensaje")
                        } else s.conversacionActiva,
                        conversaciones = if (eraUltimoMensaje) {
                            s.conversaciones.map { c ->
                                if (c.id == mensaje.conversacionId) c.copy(ultimoMensaje = "Se eliminó este mensaje") else c
                            }
                        } else s.conversaciones
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(error = "No se pudo eliminar el mensaje para todos.", exito = null) }
            }
        }
    }

    private fun validarSalaDisponibleParaEnvio(conv: Conversacion, remitenteId: String): Boolean {
        if (!conv.estaBloqueada) return true
        val aviso = if (conv.bloqueadaPorMi(remitenteId)) {
            "Bloqueaste esta conversación. Desbloquéala para volver a escribir."
        } else {
            "Por el momento no es posible enviar mensajes en esta conversación."
        }
        _state.update { it.copy(error = aviso, exito = null) }
        return false
    }

    private fun agregarMensajeEnviadoAEstado(enviado: Mensaje) {
        _state.update { s ->
            val actualizados = if (s.mensajes.any { it.id == enviado.id }) s.mensajes else s.mensajes + enviado
            s.copy(
                enviando = false,
                enviandoAdjunto = false,
                etiquetaAdjuntoEnProgreso = null,
                mensajes = actualizados,
                conversacionActiva = s.conversacionActiva?.copy(
                    ultimoMensaje = enviado.contenido,
                    actualizadoEn = enviado.creadoEn
                )
            )
        }
    }

    private fun manejarErrorEnvio(
        e: Exception,
        conv: Conversacion,
        remitenteId: String,
        mensajeDefecto: String = "No se pudo enviar el mensaje."
    ) {
        val msg = if (e.message?.contains("bloqueada", ignoreCase = true) == true) {
            if (conv.bloqueadaPorMi(remitenteId)) {
                "Bloqueaste esta conversación. Desbloquéala para volver a escribir."
            } else {
                "Por el momento no es posible enviar mensajes en esta conversación."
            }
        } else {
            mensajeDefecto
        }
        _state.update {
            it.copy(
                enviando = false,
                enviandoAdjunto = false,
                etiquetaAdjuntoEnProgreso = null,
                error = msg,
                exito = null
            )
        }
    }

    fun bloquearConversacion(conversacionId: String, miUsuarioId: String) {
        if (miUsuarioId.isBlank()) return
        notificarEscritura(miUsuarioId, false)

        viewModelScope.launch {
            _state.update { it.copy(procesandoAccionChat = true, error = null, exito = null) }
            try {
                val actualizada = withContext(Dispatchers.IO) {
                    chatRepo.actualizarBloqueoConversacion(conversacionId, miUsuarioId)
                }
                aplicarBloqueoEnEstado(
                    actualizada,
                    "Conversación bloqueada. Se suspendieron los mensajes y pedidos con este contacto."
                )
            } catch (e: Exception) {
                val detalle = if (e.message?.contains("bloqueado_por", ignoreCase = true) == true) {
                    "Ejecuta el script 03_alter_chat_realtime_bloqueos.sql en Supabase para guardar bloqueos."
                } else {
                    "No se pudo bloquear la conversación."
                }
                _state.update { it.copy(procesandoAccionChat = false, error = detalle, exito = null) }
            }
        }
    }

    fun desbloquearConversacion(conversacionId: String, miUsuarioId: String) {
        val objetivo = _state.value.conversacionActiva?.takeIf { it.id == conversacionId }
            ?: _state.value.conversaciones.firstOrNull { it.id == conversacionId }

        if (objetivo != null && objetivo.estaBloqueada && !objetivo.bloqueadaPorMi(miUsuarioId)) {
            _state.update { it.copy(error = "Por el momento no es posible modificar esta conversación.", exito = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(procesandoAccionChat = true, error = null, exito = null) }
            try {
                val actualizada = withContext(Dispatchers.IO) {
                    chatRepo.actualizarBloqueoConversacion(conversacionId, null)
                }
                aplicarBloqueoEnEstado(actualizada, "Conversación desbloqueada.")
            } catch (_: Exception) {
                _state.update { it.copy(procesandoAccionChat = false, error = "No se pudo desbloquear la conversación.", exito = null) }
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
                error = null,
                exito = mensajeExito
            )
        }
    }

    fun eliminarConversacion(conversacionId: String, onEliminado: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(procesandoAccionChat = true, error = null, exito = null) }
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
                        error = null,
                        exito = "Conversación eliminada definitivamente."
                    )
                }
                onEliminado()
            } catch (_: Exception) {
                _state.update { it.copy(procesandoAccionChat = false, error = "No se pudo eliminar la conversación.", exito = null) }
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
        roomFallbackJob?.cancel()
        roomFallbackJob = null
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
        inboxFallbackJob?.cancel()
        inboxFallbackJob = null
        usuarioObservadoId = null
        tiendaObservadaId = null
        cargaInicialBandejaLista = false
        mensajesNotificados.clear()
        ultimoAvisoBandejaPorConv.clear()
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
        roomFallbackJob?.cancel()
        inboxRealtimeJob?.cancel()
        inboxDebounceJob?.cancel()
        inboxFallbackJob?.cancel()
        localTypingIdleJob?.cancel()
        remoteTypingTimeoutJob?.cancel()
    }
}
