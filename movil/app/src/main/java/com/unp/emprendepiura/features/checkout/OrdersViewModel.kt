package com.unp.emprendepiura.features.checkout

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.core.util.CodigoEntregaUtil
import com.unp.emprendepiura.core.util.UriBytesUtil
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.repository.CatalogRepository
import com.unp.emprendepiura.data.repository.OrderRepository
import com.unp.emprendepiura.domain.model.CartItemLocal
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.MetodoPago
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.TipoEntrega
import com.unp.emprendepiura.domain.validator.MarketplaceValidator
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val catalogRepo: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    fun cargarMisOrdenes(compradorId: String) {
        if (compradorId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val (ordenes, tiendas) = withContext(Dispatchers.IO) {
                    val o = orderRepo.getOrdenesComprador(compradorId)
                    val t = catalogRepo.getEmprendimientosAprobados().associateBy { it.id }
                    o to t
                }
                val enriquecidas = ordenes.map { o ->
                    o.copy(nombreTienda = tiendas[o.emprendimientoId]?.nombreTienda ?: "Emprendimiento")
                }
                _state.update { it.copy(loading = false, misOrdenes = enriquecidas) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudieron cargar tus pedidos.") }
            }
        }
    }

    fun cargarOrdenesRecibidas(emprendimientoId: String) {
        if (emprendimientoId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
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
                _state.update { it.copy(loading = false, ordenesRecibidas = enriquecidas) }
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudieron cargar los pedidos recibidos.") }
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
            _state.update { it.copy(error = errorVal) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(procesandoCheckout = true, error = null, exito = null) }
            try {
                val comprobanteUrl = if (metodoPago == MetodoPago.YAPE_PLIN && comprobanteUri != null) {
                    withContext(Dispatchers.IO) {
                        val bytes = UriBytesUtil.comprimirImagenUri(context, comprobanteUri)
                            ?: throw IllegalStateException("No se pudo leer la imagen del comprobante.")
                        orderRepo.subirComprobantePago(compradorId, bytes)
                    }
                } else null

                withContext(Dispatchers.IO) {
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

                cargarMisOrdenes(compradorId)
                _state.update {
                    it.copy(
                        procesandoCheckout = false,
                        exito = "Pedido enviado correctamente. Puedes seguir su estado en Mis Pedidos."
                    )
                }
                onOrdenCreada()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        procesandoCheckout = false,
                        error = e.message ?: "Error al procesar el pedido."
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
            _state.update { it.copy(loading = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    orderRepo.actualizarEstadoOrden(ordenId, nuevoEstado)
                }
                if (!compradorIdRefresh.isNullOrBlank()) cargarMisOrdenes(compradorIdRefresh)
                if (!emprendimientoIdRefresh.isNullOrBlank()) cargarOrdenesRecibidas(emprendimientoIdRefresh)
                _state.update { it.copy(loading = false, exito = "Estado actualizado a ${nuevoEstado.etiqueta}.") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudo actualizar el pedido: ${e.message}") }
            }
        }
    }

    /**
     * Valida el PIN de entrega proporcionado por el comprador (estilo Uber Eats / Rappi)
     * para confirmar de forma segura que el producto fue entregado en mano sin fraudes.
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
                it.copy(error = "Código PIN incorrecto o vencido. Pide al comprador el código dinámico de 6 dígitos vigente en su pedido.")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    orderRepo.actualizarEstadoOrden(orden.id, EstadoOrden.ENTREGADO)
                }
                if (!compradorIdRefresh.isNullOrBlank()) cargarMisOrdenes(compradorIdRefresh)
                if (!emprendimientoIdRefresh.isNullOrBlank()) cargarOrdenesRecibidas(emprendimientoIdRefresh)
                _state.update {
                    it.copy(
                        loading = false,
                        exito = "¡Entrega verificada exitosamente con código PIN! Pedido completado."
                    )
                }
                onExito()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Error al confirmar entrega: ${e.message}") }
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
            _state.update { it.copy(loading = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    orderRepo.actualizarEstadoOrden(orden.id, EstadoOrden.ENTREGADO)
                }
                cargarMisOrdenes(compradorId)
                _state.update {
                    it.copy(
                        loading = false,
                        exito = "¡Confirmaste la recepción de tu pedido! Gracias por apoyar a los emprendedores UNP."
                    )
                }
                onExito()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudo confirmar la recepción: ${e.message}") }
            }
        }
    }

    fun limpiarEstadoSesion() {
        _state.value = OrdersUiState()
    }

    fun limpiarMensajes() = _state.update { it.copy(error = null, exito = null) }
}
