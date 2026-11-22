package com.unp.emprendepiura.features.store

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.util.UriBytesUtil
import com.unp.emprendepiura.data.dto.EmprendimientoWriteDto
import com.unp.emprendepiura.data.dto.ProductoWriteDto
import com.unp.emprendepiura.data.repository.StoreRepository
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.validator.MarketplaceValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Archivo: StoreViewModel.kt
 * Proposito: Gestion del emprendimiento del usuario, solicitud/reenvio de tienda,
 *            administracion de productos propios y sincronizacion Realtime de estado.
 */
data class StoreUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val miTienda: Emprendimiento? = null,
    val misProductos: List<Producto> = emptyList(),
    val error: String? = null,
    val exito: String? = null
)

class StoreViewModel(
    private val storeRepo: StoreRepository = StoreRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    private var usuarioObservadoId: String? = null
    private var storeRealtimeJob: Job? = null
    private var storeDebounceJob: Job? = null

    fun iniciarObservacionMiTienda(usuarioId: String, onEstadoCambio: () -> Unit = {}) {
        if (usuarioId.isBlank()) {
            limpiarEstadoSesion()
            return
        }
        val yaObservando = usuarioObservadoId == usuarioId && storeRealtimeJob?.isActive == true
        usuarioObservadoId = usuarioId
        cargarMiTienda(usuarioId, silencioso = yaObservando)
        if (yaObservando) return

        storeRealtimeJob?.cancel()
        storeRealtimeJob = viewModelScope.launch {
            storeRepo.observarMiEmprendimientoRealtime(usuarioId).collect {
                storeDebounceJob?.cancel()
                storeDebounceJob = launch {
                    delay(250)
                    cargarMiTienda(usuarioId, silencioso = true)
                    onEstadoCambio()
                }
            }
        }
    }

    fun cargarMiTienda(usuarioId: String, silencioso: Boolean = false) {
        if (usuarioId.isBlank()) {
            _state.update { StoreUiState() }
            return
        }
        viewModelScope.launch {
            if (!silencioso) {
                _state.update { it.copy(loading = true, error = null) }
            }
            try {
                val tienda = withContext(Dispatchers.IO) { storeRepo.getMiEmprendimiento(usuarioId) }
                val prods = if (tienda != null) {
                    withContext(Dispatchers.IO) { storeRepo.getProductosDeTienda(tienda.id) }
                } else emptyList()

                _state.update { it.copy(loading = false, miTienda = tienda, misProductos = prods) }
            } catch (_: Exception) {
                if (!silencioso) {
                    _state.update { it.copy(loading = false, error = "Error al cargar tu emprendimiento.", exito = null) }
                }
            }
        }
    }

    fun enviarSolicitudTienda(
        usuarioId: String,
        nombreTienda: String,
        descripcion: String,
        categoriaPrincipal: String,
        zonaPiura: String,
        puntoEncuentro: String,
        telefono: String,
        numeroYapePlin: String,
        onGuardado: () -> Unit
    ) {
        val err = MarketplaceValidator.validarSolicitudEmprendimiento(nombreTienda, telefono, puntoEncuentro)
        if (err != null) {
            _state.update { it.copy(error = err, exito = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, exito = null) }
            try {
                val creada = withContext(Dispatchers.IO) {
                    storeRepo.registrarOSolicitarTienda(
                        EmprendimientoWriteDto(
                            usuarioId = usuarioId,
                            nombreTienda = nombreTienda.trim(),
                            descripcion = descripcion.trim(),
                            categoriaPrincipal = categoriaPrincipal,
                            zonaPiura = zonaPiura,
                            puntoEncuentroHabitual = puntoEncuentro.trim(),
                            telefonoContacto = telefono.trim(),
                            numeroYapePlin = numeroYapePlin.trim().ifBlank { telefono.trim() },
                            estado = "PENDIENTE",
                            motivoRevision = ""
                        )
                    )
                }
                _state.update {
                    it.copy(
                        saving = false,
                        miTienda = creada,
                        error = null,
                        exito = "Solicitud enviada. Esperando respuesta del administrador."
                    )
                }
                onGuardado()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        error = "No se pudo registrar la solicitud: ${e.message}",
                        exito = null
                    )
                }
            }
        }
    }

    fun guardarProducto(
        context: Context,
        productoExistente: Producto?,
        emprendimientoId: String,
        titulo: String,
        descripcion: String,
        precioTexto: String,
        stockTexto: String,
        categoriaId: String,
        zonaPiura: String,
        imagenUri: Uri?,
        onGuardado: () -> Unit
    ) {
        val err = MarketplaceValidator.validarProducto(titulo, precioTexto, stockTexto, categoriaId)
        if (err != null) {
            _state.update { it.copy(error = err, exito = null) }
            return
        }

        val precio = precioTexto.replace(',', '.').toDouble()
        val stock = stockTexto.toInt()

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, exito = null) }
            try {
                val imagenUrlFinal = if (imagenUri != null) {
                    withContext(Dispatchers.IO) {
                        val bytes = UriBytesUtil.comprimirImagenUri(context, imagenUri)
                            ?: throw IllegalStateException("No se pudo procesar la foto seleccionada.")
                        storeRepo.subirImagenProducto(emprendimientoId, bytes)
                    }
                } else {
                    productoExistente?.imagenUrl?.takeIf { it.isNotBlank() }
                }

                val dto = ProductoWriteDto(
                    emprendimientoId = emprendimientoId,
                    categoriaId = categoriaId,
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim(),
                    precio = precio,
                    stock = stock,
                    zonaPiura = zonaPiura,
                    imagenUrl = imagenUrlFinal,
                    estado = productoExistente?.estado?.name ?: EstadoProducto.ACTIVO.name
                )

                withContext(Dispatchers.IO) {
                    if (productoExistente == null) storeRepo.crearProducto(dto)
                    else storeRepo.actualizarProducto(productoExistente.id, dto)
                }

                val actualizados = withContext(Dispatchers.IO) { storeRepo.getProductosDeTienda(emprendimientoId) }
                _state.update {
                    it.copy(
                        saving = false,
                        misProductos = actualizados,
                        error = null,
                        exito = if (productoExistente == null) "Producto publicado." else "Producto actualizado."
                    )
                }
                onGuardado()
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, error = "No se pudo guardar el producto: ${e.message}", exito = null)
                }
            }
        }
    }

    fun toggleEstadoProducto(producto: Producto, emprendimientoId: String) {
        val siguiente = if (producto.estado == EstadoProducto.ACTIVO) {
            EstadoProducto.PAUSADO
        } else {
            EstadoProducto.ACTIVO
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { storeRepo.cambiarEstadoProducto(producto.id, siguiente) }
                val actualizados = withContext(Dispatchers.IO) { storeRepo.getProductosDeTienda(emprendimientoId) }
                _state.update { it.copy(misProductos = actualizados) }
            }
        }
    }

    fun limpiarEstadoSesion() {
        storeRealtimeJob?.cancel()
        storeRealtimeJob = null
        storeDebounceJob?.cancel()
        storeDebounceJob = null
        usuarioObservadoId = null
        _state.value = StoreUiState()
    }

    fun limpiarMensajes() = _state.update { it.copy(error = null, exito = null) }

    override fun onCleared() {
        super.onCleared()
        storeRealtimeJob?.cancel()
        storeDebounceJob?.cancel()
    }
}
