package com.unp.emprendepiura.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.data.repository.AdminRepository
import com.unp.emprendepiura.domain.model.AdminDashboardStats
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.model.Usuario
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
 * Archivo: AdminViewModel.kt
 * Proposito: Estado y acciones del Panel Administrativo: metricas interactivas,
 *            control de usuarios (habilitar/deshabilitar), tiendas, catalogo y pedidos.
 */
data class AdminUiState(
    val loading: Boolean = false,
    val stats: AdminDashboardStats = AdminDashboardStats(),
    val usuarios: List<Usuario> = emptyList(),
    val emprendimientos: List<Emprendimiento> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val ordenes: List<Orden> = emptyList(),
    val error: String? = null,
    val exito: String? = null
)

class AdminViewModel(
    private val adminRepo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private var adminRealtimeJob: Job? = null
    private var adminDebounceJob: Job? = null

    fun iniciarObservacionAdmin() {
        cargarTodo()
        if (adminRealtimeJob?.isActive == true) return
        adminRealtimeJob = viewModelScope.launch {
            adminRepo.observarCambiosAdminRealtime().collect {
                adminDebounceJob?.cancel()
                adminDebounceJob = launch {
                    delay(280)
                    cargarTodo(silencioso = true)
                }
            }
        }
    }

    fun detenerObservacionAdmin() {
        adminRealtimeJob?.cancel()
        adminRealtimeJob = null
        adminDebounceJob?.cancel()
        adminDebounceJob = null
    }

    fun cargarTodo(silencioso: Boolean = false) {
        viewModelScope.launch {
            if (!silencioso) {
                _state.update { it.copy(loading = true, error = null) }
            }
            try {
                val snap = withContext(Dispatchers.IO) { adminRepo.getSnapshotCompleto() }
                _state.update {
                    it.copy(
                        loading = false,
                        stats = snap.stats,
                        usuarios = snap.usuarios,
                        emprendimientos = snap.emprendimientos,
                        categorias = snap.categorias,
                        productos = snap.productos,
                        ordenes = snap.ordenes
                    )
                }
            } catch (_: Exception) {
                if (!silencioso) {
                    _state.update {
                        it.copy(loading = false, error = "No se pudo cargar el panel administrativo.", exito = null)
                    }
                }
            }
        }
    }

    fun toggleUsuarioActivo(usuario: Usuario, miAdminId: String) {
        if (usuario.id == miAdminId) {
            _state.update {
                it.copy(error = "No puedes deshabilitar tu propia cuenta de administrador.", exito = null)
            }
            return
        }
        val nuevoActivo = !usuario.activo
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, exito = null) }
            try {
                withContext(Dispatchers.IO) {
                    adminRepo.cambiarEstadoActivoUsuario(usuario.id, nuevoActivo)
                }
                cargarTodo()
                val estadoTexto = if (nuevoActivo) "habilitado" else "deshabilitado"
                _state.update {
                    it.copy(error = null, exito = "Usuario ${usuario.nombre} $estadoTexto correctamente.")
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(loading = false, error = "No se pudo actualizar el estado del usuario.", exito = null)
                }
            }
        }
    }

    fun resolverEmprendimiento(
        tienda: Emprendimiento,
        nuevoEstado: EstadoEmprendimiento,
        onResuelto: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, exito = null) }
            try {
                withContext(Dispatchers.IO) {
                    adminRepo.resolverEmprendimiento(tienda.id, nuevoEstado)
                }
                cargarTodo()
                _state.update {
                    it.copy(error = null, exito = "${tienda.nombreTienda}: ${nuevoEstado.etiqueta}")
                }
                onResuelto()
            } catch (_: Exception) {
                _state.update {
                    it.copy(loading = false, error = "No se pudo actualizar la tienda.", exito = null)
                }
            }
        }
    }

    fun crearCategoria(nombre: String, onCreada: () -> Unit) {
        val clean = nombre.trim()
        if (clean.length < 3) {
            _state.update { it.copy(error = "Nombre de categoría demasiado corto.", exito = null) }
            return
        }
        viewModelScope.launch {
            runCatching {
                val nextOrden = (_state.value.categorias.maxOfOrNull { it.orden } ?: 0) + 1
                withContext(Dispatchers.IO) { adminRepo.crearCategoria(clean, nextOrden) }
            }.onSuccess {
                cargarTodo()
                onCreada()
                _state.update { it.copy(error = null, exito = "Categoría \"$clean\" creada.") }
            }.onFailure {
                _state.update {
                    it.copy(error = "No se pudo crear la categoría (posible duplicado).", exito = null)
                }
            }
        }
    }

    fun toggleCategoria(cat: Categoria, onCambio: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { adminRepo.toggleCategoriaActiva(cat.id, !cat.activa) }
            }.onSuccess {
                cargarTodo()
                onCambio()
            }
        }
    }

    fun moderarProducto(prod: Producto, onCambio: () -> Unit) {
        val siguiente = if (prod.estado == EstadoProducto.ACTIVO) {
            EstadoProducto.OCULTO_ADMIN
        } else {
            EstadoProducto.ACTIVO
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { adminRepo.moderarProducto(prod.id, siguiente) }
            }.onSuccess {
                cargarTodo()
                onCambio()
                _state.update { it.copy(error = null, exito = "Producto actualizado a ${siguiente.etiqueta}.") }
            }
        }
    }

    fun limpiarMensajes() = _state.update { it.copy(error = null, exito = null) }

    override fun onCleared() {
        super.onCleared()
        detenerObservacionAdmin()
    }
}
