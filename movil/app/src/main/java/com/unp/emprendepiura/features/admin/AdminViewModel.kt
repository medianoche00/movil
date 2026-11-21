package com.unp.emprendepiura.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.data.repository.AdminRepository
import com.unp.emprendepiura.domain.model.AdminDashboardStats
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdminUiState(
    val loading: Boolean = false,
    val stats: AdminDashboardStats = AdminDashboardStats(),
    val emprendimientos: List<Emprendimiento> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val error: String? = null,
    val exito: String? = null
)

class AdminViewModel(
    private val adminRepo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    fun cargarTodo() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val (stats, tiendas, cats, prods) = withContext(Dispatchers.IO) {
                    val s = adminRepo.getDashboardStats()
                    val t = adminRepo.getTodosEmprendimientos()
                        .sortedBy { if (it.estado == EstadoEmprendimiento.PENDIENTE) 0 else 1 }
                    val c = adminRepo.getTodasCategorias()
                    val p = adminRepo.getTodosProductosParaModeracion()
                    Quad(s, t, c, p)
                }
                _state.update {
                    it.copy(
                        loading = false,
                        stats = stats,
                        emprendimientos = tiendas,
                        categorias = cats,
                        productos = prods
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudo cargar el panel administrativo.") }
            }
        }
    }

    fun resolverEmprendimiento(
        tienda: Emprendimiento,
        nuevoEstado: EstadoEmprendimiento,
        onResuelto: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    adminRepo.resolverEmprendimiento(tienda.id, nuevoEstado)
                }
                cargarTodo()
                _state.update {
                    it.copy(exito = "${tienda.nombreTienda}: ${nuevoEstado.etiqueta}")
                }
                onResuelto()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "No se pudo actualizar la tienda.") }
            }
        }
    }

    fun crearCategoria(nombre: String, onCreada: () -> Unit) {
        val clean = nombre.trim()
        if (clean.length < 3) {
            _state.update { it.copy(error = "Nombre de categoría demasiado corto.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                val nextOrden = (_state.value.categorias.maxOfOrNull { it.orden } ?: 0) + 1
                withContext(Dispatchers.IO) { adminRepo.crearCategoria(clean, nextOrden) }
            }.onSuccess {
                cargarTodo()
                onCreada()
                _state.update { it.copy(exito = "Categoría \"$clean\" creada.") }
            }.onFailure {
                _state.update { it.copy(error = "No se pudo crear la categoría (posible duplicado).") }
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
                _state.update { it.copy(exito = "Producto actualizado a ${siguiente.etiqueta}.") }
            }
        }
    }

    fun limpiarMensajes() = _state.update { it.copy(error = null, exito = null) }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
