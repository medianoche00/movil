package com.unp.emprendepiura.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.repository.CatalogRepository
import com.unp.emprendepiura.data.repository.ChatRepository
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Mensaje
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
    val conversaciones: List<Conversacion> = emptyList(),
    val conversacionActiva: Conversacion? = null,
    val tiendaContraparte: Emprendimiento? = null,
    val mensajes: List<Mensaje> = emptyList(),
    val conversacionesBloqueadas: Set<String> = emptySet(),
    val contraparteEscribiendo: Boolean = false,
    val error: String? = null,
    val exito: String? = null
)

class ChatViewModel(
    private val chatRepo: ChatRepository = ChatRepository(),
    private val catalogRepo: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var realtimeJob: Job? = null
    private var typingSimulatorJob: Job? = null

    fun cargarConversaciones(usuarioId: String, miEmprendimientoId: String?) {
        if (usuarioId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loadingConversaciones = true, error = null) }
            try {
                val lista = withContext(Dispatchers.IO) {
                    val convs = chatRepo.getConversacionesUsuario(usuarioId, miEmprendimientoId)
                    val tiendas = catalogRepo.getEmprendimientosAprobados().associateBy { it.id }
                    val usuarios = runCatching {
                        SupaClient.client.from("usuarios").select().decodeList<UsuarioRowDto>()
                    }.getOrDefault(emptyList()).associateBy { it.id }

                    convs.map { c ->
                        val contraparte = if (c.compradorId == usuarioId) {
                            tiendas[c.emprendimientoId]?.nombreTienda ?: "Emprendimiento"
                        } else {
                            usuarios[c.compradorId]?.nombre ?: "Comprador"
                        }
                        c.copy(nombreContraparte = contraparte)
                    }
                }
                _state.update { it.copy(loadingConversaciones = false, conversaciones = lista) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingConversaciones = false, error = "Error al cargar chats.") }
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
                abrirSala(conv)
                onAbierto()
            } catch (e: Exception) {
                _state.update { it.copy(loadingMensajes = false, error = "No se pudo iniciar la conversación.") }
            }
        }
    }

    fun abrirSala(conversacion: Conversacion) {
        realtimeJob?.cancel()
        typingSimulatorJob?.cancel()
        _state.update {
            it.copy(
                conversacionActiva = conversacion,
                mensajes = emptyList(),
                loadingMensajes = true,
                tiendaContraparte = null,
                contraparteEscribiendo = false,
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
                _state.update { it.copy(loadingMensajes = false, mensajes = msgs, tiendaContraparte = tienda) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingMensajes = false, error = "No se pudieron cargar los mensajes.") }
            }
        }

        realtimeJob = viewModelScope.launch {
            chatRepo.observarMensajesRealtime(conversacion.id).collect { nuevo ->
                _state.update { s ->
                    if (s.mensajes.any { it.id == nuevo.id }) s
                    else s.copy(mensajes = s.mensajes + nuevo)
                }
            }
        }
    }

    fun enviarMensaje(remitenteId: String, texto: String) {
        val conv = _state.value.conversacionActiva ?: return
        val convId = conv.id
        if (_state.value.conversacionesBloqueadas.contains(convId)) {
            _state.update { it.copy(error = "Esta conversación está bloqueada. Desbloquéala para chatear.") }
            return
        }

        val clean = texto.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(enviando = true, error = null) }
            try {
                val enviado = withContext(Dispatchers.IO) {
                    chatRepo.enviarMensaje(convId, remitenteId, clean)
                }
                _state.update { s ->
                    val actualizados = if (s.mensajes.any { it.id == enviado.id }) s.mensajes else s.mensajes + enviado
                    s.copy(enviando = false, mensajes = actualizados)
                }

                // Simular presencia en vivo estilo WhatsApp / Telegram
                iniciarSimulacionRespuestaContraparte(convId, remitenteId)
            } catch (e: Exception) {
                _state.update { it.copy(enviando = false, error = "No se pudo enviar el mensaje.") }
            }
        }
    }

    /**
     * Simula la presencia de "Escribiendo..." con los 3 puntos animados tipicos de WhatsApp/Telegram
     * para una sensacion dinamica e hiperrealista de conversacion.
     */
    private fun iniciarSimulacionRespuestaContraparte(convId: String, miUsuarioId: String) {
        typingSimulatorJob?.cancel()
        typingSimulatorJob = viewModelScope.launch {
            delay(1200)
            if (_state.value.conversacionActiva?.id == convId) {
                _state.update { it.copy(contraparteEscribiendo = true) }
                delay(3000)
                _state.update { it.copy(contraparteEscribiendo = false) }
            }
        }
    }

    fun notificarUsuarioEscribiendo(estaEscribiendo: Boolean) {
        // Puede usarse para sincronizar estado de escritura local
    }

    fun bloquearConversacion(conversacionId: String) {
        _state.update {
            it.copy(
                conversacionesBloqueadas = it.conversacionesBloqueadas + conversacionId,
                exito = "Conversación bloqueada. No recibirás nuevos avisos."
            )
        }
    }

    fun desbloquearConversacion(conversacionId: String) {
        _state.update {
            it.copy(
                conversacionesBloqueadas = it.conversacionesBloqueadas - conversacionId,
                exito = "Conversación desbloqueada."
            )
        }
    }

    fun eliminarConversacion(conversacionId: String, onEliminado: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatRepo.eliminarConversacion(conversacionId)
                }
                _state.update { s ->
                    val filtradas = s.conversaciones.filterNot { it.id == conversacionId }
                    val activa = if (s.conversacionActiva?.id == conversacionId) null else s.conversacionActiva
                    s.copy(conversaciones = filtradas, conversacionActiva = activa, exito = "Conversación eliminada con éxito.")
                }
                onEliminado()
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo eliminar la conversación: ${e.message}") }
            }
        }
    }

    fun cerrarSala() {
        realtimeJob?.cancel()
        realtimeJob = null
        typingSimulatorJob?.cancel()
        typingSimulatorJob = null
        _state.update { it.copy(conversacionActiva = null, mensajes = emptyList(), tiendaContraparte = null, contraparteEscribiendo = false) }
    }

    fun limpiarMensajes() {
        _state.update { it.copy(error = null, exito = null) }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        typingSimulatorJob?.cancel()
    }
}
