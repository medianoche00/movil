package com.unp.emprendepiura.features.checkout

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.network.RealtimeChannelHelper
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.core.notifications.EmprendeNotificationHelper
import com.unp.emprendepiura.core.util.CodigoEntregaUtil
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.core.util.UriBytesUtil
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.repository.CatalogRepository
import com.unp.emprendepiura.data.repository.ChatRepository
import com.unp.emprendepiura.data.repository.OrderRepository
import com.unp.emprendepiura.domain.model.CartItemLocal
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.MetodoPago
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.OrdenRealtimeEvent
import com.unp.emprendepiura.domain.model.TipoEntrega
import com.unp.emprendepiura.domain.validator.MarketplaceValidator
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
 * Archivo: OrdersViewModel.kt
 * Proposito: Checkout atomico protegido ante bloqueos, seguimiento dual (Mis Compras / Por Entregar),
 *            sincronizacion Realtime de estados y notificaciones nativas de pedidos en vivo.
 */
data class OrdersUiState(
    val loading: Boolean = false,
    val procesandoCheckout: Boolean = false,
    val misOrdenes: List<Orden> = emptyList(),
    val ordenesRecibidas: List<Orden> = emptyList(),
    val error: String? = null,
    val exito: String? = null
) {
    val entregasPendientesCount: Int
        get() = ordenesRecibidas.count {
            it.estado != EstadoOrden.ENTREGADO && it.estado != EstadoOrden.CANCELADO
        }
}

class OrdersViewModel(
    private val orderRepo: OrderRepository = OrderRepository(),
    private val catalogRepo: CatalogRepository = CatalogRepository(),
    private val chatRepo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var usuarioObservadoId: String? = null
    private var tiendaObservadaId: String? = null

    private var ordersRealtimeJob: Job? = null
    private var ordersDebounceJob: Job? = null
    private var ordersFallbackJob: Job? = null

    private val eventosNotificados = LinkedHashSet<String>()
    private val estadosPreviosCompra = mutableMapOf<String, EstadoOrden>()
    private val estadosPreviosVenta = mutableMapOf<String, EstadoOrden>()
    private var cargaInicialCompraLista = false
    private var cargaInicialVentaLista = false

    fun iniciarObservacionOrdenesRealtime(
        context: Context,
        compradorId: String,
        miEmprendimientoId: String?,
        onCatalogoCambio: () -> Unit
    ) {
        if (compradorId.isBlank()) return
        appContext = context.applicationContext

        val mismoContexto = usuarioObservadoId == compradorId &&
            tiendaObservadaId == miEmprendimientoId &&
            ordersRealtimeJob?.isActive == true

        if (usuarioObservadoId != compradorId) {
            estadosPreviosCompra.clear()
            estadosPreviosVenta.clear()
            eventosNotificados.clear()
            cargaInicialCompraLista = false
            cargaInicialVentaLista = false
        } else if (tiendaObservadaId != miEmprendimientoId) {
            estadosPreviosVenta.clear()
            cargaInicialVentaLista = false
        }

        usuarioObservadoId = compradorId
        tiendaObservadaId = miEmprendimientoId

        cargarMisOrdenes(compradorId, silencioso = mismoContexto)
        if (!miEmprendimientoId.isNullOrBlank()) {
            cargarOrdenesRecibidas(miEmprendimientoId, silencioso = mismoContexto)
        }

        if (mismoContexto) return

        ordersRealtimeJob?.cancel()
        ordersRealtimeJob = viewModelScope.launch {
            orderRepo.observarOrdenesRealtime(compradorId, miEmprendimientoId).collect { evento ->
                when (evento) {
                    is OrdenRealtimeEvent.NuevaOrden -> {
                        notificarNuevaOrdenEntrante(evento.orden, compradorId, miEmprendimientoId)
                        programarRefrescoOrdenes(compradorId, miEmprendimientoId, onCatalogoCambio)
                    }
                    is OrdenRealtimeEvent.OrdenActualizada -> {
                        notificarCambioEstadoOrden(evento.orden, compradorId, miEmprendimientoId)
                        programarRefrescoOrdenes(compradorId, miEmprendimientoId, onCatalogoCambio)
                    }
                    OrdenRealtimeEvent.Reconectado -> {
                        programarRefrescoOrdenes(compradorId, miEmprendimientoId, onCatalogoCambio)
                    }
                }
            }
        }

        // Red de respaldo: sincroniza pedidos nuevos y cambios de estado si falla el WebSocket
        ordersFallbackJob?.cancel()
        ordersFallbackJob = viewModelScope.launch {
            while (isActive) {
                val esperaMs = if (RealtimeChannelHelper.estaConectado()) 16_000L else 5_500L
                delay(esperaMs)
                cargarMisOrdenes(compradorId, silencioso = true, detectarCambiosParaNotificar = true)
                if (!miEmprendimientoId.isNullOrBlank()) {
                    cargarOrdenesRecibidas(miEmprendimientoId, silencioso = true, detectarCambiosParaNotificar = true)
                }
            }
        }
    }

    private fun programarRefrescoOrdenes(
        compradorId: String,
        miEmprendimientoId: String?,
        onCatalogoCambio: () -> Unit
    ) {
        ordersDebounceJob?.cancel()
        ordersDebounceJob = viewModelScope.launch {
            delay(220)
            cargarMisOrdenes(compradorId, silencioso = true)
            if (!miEmprendimientoId.isNullOrBlank()) {
                cargarOrdenesRecibidas(miEmprendimientoId, silencioso = true)
            }
            onCatalogoCambio()
        }
    }

    private fun registrarEventoNotificado(clave: String): Boolean {
        if (clave in eventosNotificados) return false
        eventosNotificados.add(clave)
        if (eventosNotificados.size > 200) {
            val primero = eventosNotificados.firstOrNull()
            if (primero != null) eventosNotificados.remove(primero)
        }
        return true
    }

    private fun notificarNuevaOrdenEntrante(
        orden: Orden,
        miUsuarioId: String,
        miEmprendimientoId: String?
    ) {
        val ctx = appContext ?: return
        val esParaMiTienda = !miEmprendimientoId.isNullOrBlank() &&
            orden.emprendimientoId == miEmprendimientoId &&
            orden.compradorId != miUsuarioId
        if (!esParaMiTienda) return

        val clave = "NEW_${orden.id}"
        if (!registrarEventoNotificado(clave)) return
        registrarEventoNotificado("${orden.id}_${orden.estado.name}")

        val montoTexto = if (orden.total > 0.0) {
            " por ${PiuraConstants.formatearSoles(orden.total)}"
        } else {
            ""
        }
        EmprendeNotificationHelper.mostrarNotificacionPedido(
            context = ctx,
            ordenId = orden.id,
            titulo = "Nuevo pedido recibido · ${orden.codigoOrden}",
            detalle = "Tienes un nuevo pedido$montoTexto pendiente de confirmación."
        )
    }

    private fun notificarCambioEstadoOrden(
        orden: Orden,
        miUsuarioId: String,
        miEmprendimientoId: String?
    ) {
        val ctx = appContext ?: return
        val clave = "${orden.id}_${orden.estado.name}"
        if (!registrarEventoNotificado(clave)) return

        val soyComprador = orden.compradorId == miUsuarioId
        val soyVendedor = !miEmprendimientoId.isNullOrBlank() &&
            orden.emprendimientoId == miEmprendimientoId &&
            !soyComprador

        when {
            soyComprador -> {
                val (titulo, detalle) = when (orden.estado) {
                    EstadoOrden.CONFIRMADO -> "Pedido confirmado · ${orden.codigoOrden}" to
                        "El emprendedor confirmó tu pedido y lo está preparando."
                    EstadoOrden.EN_CAMINO -> "Pedido listo / en camino · ${orden.codigoOrden}" to
                        "Tu pedido ya está listo o en camino. Ten a la mano tu código dinámico de entrega."
                    EstadoOrden.ENTREGADO -> "Pedido entregado · ${orden.codigoOrden}" to
                        "Tu pedido fue marcado como entregado. Gracias por apoyar a los emprendedores locales."
                    EstadoOrden.CANCELADO -> "Pedido cancelado · ${orden.codigoOrden}" to
                        "El pedido ${orden.codigoOrden} fue cancelado."
                    EstadoOrden.PENDIENTE -> return
                }
                EmprendeNotificationHelper.mostrarNotificacionPedido(
                    context = ctx,
                    ordenId = orden.id,
                    titulo = titulo,
                    detalle = detalle
                )
            }
            soyVendedor -> {
                val (titulo, detalle) = when (orden.estado) {
                    EstadoOrden.ENTREGADO -> "Entrega confirmada · ${orden.codigoOrden}" to
                        "El comprador confirmó la recepción del pedido ${orden.codigoOrden}."
                    EstadoOrden.CANCELADO -> "Pedido cancelado · ${orden.codigoOrden}" to
                        "El pedido ${orden.codigoOrden} fue cancelado y su stock fue restaurado."
                    else -> return
                }
                EmprendeNotificationHelper.mostrarNotificacionPedido(
                    context = ctx,
                    ordenId = orden.id,
                    titulo = titulo,
                    detalle = detalle
                )
            }
        }
    }

    fun cargarMisOrdenes(
        compradorId: String,
        silencioso: Boolean = false,
        detectarCambiosParaNotificar: Boolean = false
    ) {
        if (compradorId.isBlank()) return
        viewModelScope.launch {
            if (!silencioso) {
                _state.update { it.copy(loading = true, error = null) }
            }
            try {
                val (ordenes, tiendas) = withContext(Dispatchers.IO) {
                    val o = orderRepo.getOrdenesComprador(compradorId)
                    val t = catalogRepo.getEmprendimientosAprobados().associateBy { it.id }
                    o to t
                }
                val enriquecidas = ordenes.map { o ->
                    o.copy(nombreTienda = tiendas[o.emprendimientoId]?.nombreTienda ?: "Emprendimiento")
                }

                if (detectarCambiosParaNotificar && cargaInicialCompraLista) {
                    enriquecidas.forEach { orden ->
                        val estadoPrevio = estadosPreviosCompra[orden.id]
                        if (estadoPrevio != null && estadoPrevio != orden.estado) {
                            notificarCambioEstadoOrden(orden, compradorId, tiendaObservadaId)
                        }
                    }
                }
                enriquecidas.forEach { estadosPreviosCompra[it.id] = it.estado }
                cargaInicialCompraLista = true

                _state.update { it.copy(loading = false, misOrdenes = enriquecidas) }
            } catch (_: Exception) {
                if (!silencioso) {
                    _state.update { it.copy(loading = false, error = "No se pudieron cargar tus pedidos.") }
                }
            }
        }
    }

    fun cargarOrdenesRecibidas(
        emprendimientoId: String,
        silencioso: Boolean = false,
        detectarCambiosParaNotificar: Boolean = false
    ) {
        if (emprendimientoId.isBlank()) return
        viewModelScope.launch {
            if (!silencioso) {
                _state.update { it.copy(loading = true, error = null) }
            }
            try {
                val enriquecidas = withContext(Dispatchers.IO) {
                    val ordenes = orderRepo.getOrdenesEmprendimiento(emprendimientoId)
                    val usuarios = runCatching {
                        SupaClient.client.from("usuarios").select().decodeList<UsuarioRowDto>()
                    }.getOrDefault(emptyList()).associateBy { it.id }

                    ordenes.map { o ->
                        o.copy(nombreComprador = usuarios[o.compradorId]?.nombre ?: "Comprador")
                    }
                }

                val miUid = usuarioObservadoId.orEmpty()
                if (detectarCambiosParaNotificar && cargaInicialVentaLista && miUid.isNotBlank()) {
                    enriquecidas.forEach { orden ->
                        val estadoPrevio = estadosPreviosVenta[orden.id]
                        when {
                            estadoPrevio == null -> notificarNuevaOrdenEntrante(orden, miUid, emprendimientoId)
                            estadoPrevio != orden.estado -> notificarCambioEstadoOrden(orden, miUid, emprendimientoId)
                        }
                    }
                }
                enriquecidas.forEach { estadosPreviosVenta[it.id] = it.estado }
                cargaInicialVentaLista = true

                _state.update { it.copy(loading = false, ordenesRecibidas = enriquecidas) }
            } catch (_: Exception) {
                if (!silencioso) {
                    _state.update { it.copy(loading = false, error = "No se pudieron cargar los pedidos recibidos.") }
                }
            }
        }
    }

    fun confirmarPedidoTienda(
        context: Context,
        compradorId: String,
        emprendimientoId: String,
        tipoEntrega: TipoEntrega,
        direccionOPunto: String,
        metodoPago: MetodoPago,
        comprobanteUri: Uri?,
        codigoOperacion: String,
        notas: String,
        items: List<CartItemLocal>,
        onOrdenCreada: () -> Unit
    ) {
        val errorVal = MarketplaceValidator.validarCheckout(
            direccionOPunto = direccionOPunto,
            metodoPago = metodoPago,
            tieneComprobante = comprobanteUri != null
        )
        if (errorVal != null) {
            _state.update { it.copy(error = errorVal, exito = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(procesandoCheckout = true, error = null, exito = null) }
            try {
                val bloqueado = withContext(Dispatchers.IO) {
                    chatRepo.existeBloqueoConTienda(compradorId, emprendimientoId)
                }
                if (bloqueado) {
                    _state.update {
                        it.copy(
                            procesandoCheckout = false,
                            error = "Este emprendimiento no está disponible para realizar pedidos en este momento."
                        )
                    }
                    return@launch
                }

                val comprobanteUrl = if (metodoPago == MetodoPago.YAPE_PLIN && comprobanteUri != null) {
                    withContext(Dispatchers.IO) {
                        val bytes = UriBytesUtil.comprimirImagenUri(context, comprobanteUri)
                            ?: throw IllegalStateException("No se pudo leer la imagen del comprobante.")
                        orderRepo.subirComprobantePago(compradorId, bytes)
                    }
                } else null

                val ordenId = withContext(Dispatchers.IO) {
                    orderRepo.crearOrdenAtomica(
                        compradorId = compradorId,
                        emprendimientoId = emprendimientoId,
                        tipoEntrega = tipoEntrega,
                        direccionOPunto = direccionOPunto,
                        metodoPago = metodoPago,
                        comprobanteUrl = comprobanteUrl,
                        codigoOperacion = codigoOperacion,
                        notas = notas,
                        items = items
                    )
                }

                registrarEventoNotificado("NEW_$ordenId")
                registrarEventoNotificado("${ordenId}_${EstadoOrden.PENDIENTE.name}")

                cargarMisOrdenes(compradorId, silencioso = true)
                _state.update {
                    it.copy(
                        procesandoCheckout = false,
                        exito = "Pedido enviado correctamente. Puedes seguir su estado en Mis Pedidos."
                    )
                }
                onOrdenCreada()
            } catch (e: Exception) {
                val msg = if (e.message?.contains("disponible para realizar pedidos", ignoreCase = true) == true) {
                    "Este emprendimiento no está disponible para realizar pedidos en este momento."
                } else {
                    e.message ?: "Error al procesar el pedido."
                }
                _state.update {
                    it.copy(
                        procesandoCheckout = false,
                        error = msg
                    )
                }
            }
        }
    }

    fun cambiarEstadoOrden(
        ordenId: String,
        nuevoEstado: EstadoOrden,
        compradorIdRefresh: String? = null,
        emprendimientoIdRefresh: String? = null
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, exito = null) }
            try {
                registrarEventoNotificado("${ordenId}_${nuevoEstado.name}")
                withContext(Dispatchers.IO) {
                    orderRepo.actualizarEstadoOrden(ordenId, nuevoEstado)
                }
                if (!compradorIdRefresh.isNullOrBlank()) cargarMisOrdenes(compradorIdRefresh, silencioso = true)
                if (!emprendimientoIdRefresh.isNullOrBlank()) cargarOrdenesRecibidas(emprendimientoIdRefresh, silencioso = true)
                _state.update { it.copy(loading = false, error = null, exito = "Estado actualizado a ${nuevoEstado.etiqueta}.") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudo actualizar el pedido: ${e.message}", exito = null) }
            }
        }
    }

    /**
     * Valida el PIN dinamico de 6 digitos proporcionado por el comprador para confirmar
     * de forma segura que el producto fue entregado en mano sin fraudes.
     */
    fun confirmarEntregaConPin(
        orden: Orden,
        pinIngresado: String,
        compradorIdRefresh: String? = null,
        emprendimientoIdRefresh: String? = null,
        onExito: () -> Unit
    ) {
        val valido = CodigoEntregaUtil.validarCodigoEntrega(orden.id, orden.codigoOrden, pinIngresado)
        if (!valido) {
            _state.update {
                it.copy(
                    error = "Código PIN incorrecto o vencido. Pide al comprador el código dinámico de 6 dígitos vigente en su pedido.",
                    exito = null
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, exito = null) }
            try {
                registrarEventoNotificado("${orden.id}_${EstadoOrden.ENTREGADO.name}")
                withContext(Dispatchers.IO) {
                    orderRepo.actualizarEstadoOrden(orden.id, EstadoOrden.ENTREGADO)
                }
                if (!compradorIdRefresh.isNullOrBlank()) cargarMisOrdenes(compradorIdRefresh, silencioso = true)
                if (!emprendimientoIdRefresh.isNullOrBlank()) cargarOrdenesRecibidas(emprendimientoIdRefresh, silencioso = true)
                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        exito = "Entrega verificada exitosamente con código PIN. Pedido completado."
                    )
                }
                onExito()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Error al confirmar entrega: ${e.message}", exito = null) }
            }
        }
    }

    /**
     * Permite al comprador confirmar que recibio su pedido en mano.
     */
    fun confirmarRecepcionComprador(
        orden: Orden,
        compradorId: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, exito = null) }
            try {
                registrarEventoNotificado("${orden.id}_${EstadoOrden.ENTREGADO.name}")
                withContext(Dispatchers.IO) {
                    orderRepo.actualizarEstadoOrden(orden.id, EstadoOrden.ENTREGADO)
                }
                cargarMisOrdenes(compradorId, silencioso = true)
                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        exito = "Confirmaste la recepción de tu pedido. Gracias por apoyar a los emprendedores UNP."
                    )
                }
                onExito()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudo confirmar la recepción: ${e.message}", exito = null) }
            }
        }
    }

    fun detenerObservacionRealtime() {
        ordersRealtimeJob?.cancel()
        ordersRealtimeJob = null
        ordersDebounceJob?.cancel()
        ordersDebounceJob = null
        ordersFallbackJob?.cancel()
        ordersFallbackJob = null
        usuarioObservadoId = null
        tiendaObservadaId = null
    }

    fun limpiarEstadoSesion() {
        detenerObservacionRealtime()
        estadosPreviosCompra.clear()
        estadosPreviosVenta.clear()
        eventosNotificados.clear()
        cargaInicialCompraLista = false
        cargaInicialVentaLista = false
        _state.value = OrdersUiState()
    }

    fun limpiarMensajes() = _state.update { it.copy(error = null, exito = null) }

    override fun onCleared() {
        super.onCleared()
        detenerObservacionRealtime()
    }
}
