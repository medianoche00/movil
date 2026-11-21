package com.unp.emprendepiura.features.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.data.repository.CatalogRepository
import com.unp.emprendepiura.domain.model.CartItemLocal
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CatalogUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val infoBanner: String? = null,
    val categorias: List<Categoria> = emptyList(),
    val tiendas: Map<String, Emprendimiento> = emptyMap(),
    val productos: List<Producto> = emptyList(),
    val query: String = "",
    val categoriaSeleccionadaId: String? = null,
    val zonaSeleccionada: String? = null,
    val cartItems: List<CartItemLocal> = emptyList()
) {
    val productosFiltrados: List<Producto>
        get() = productos.filter { p ->
            val matchCat = categoriaSeleccionadaId == null || p.categoriaId == categoriaSeleccionadaId
            val matchZona = zonaSeleccionada == null || p.zonaPiura.equals(zonaSeleccionada, ignoreCase = true)
            val q = query.trim()
            val matchQuery = q.isEmpty() ||
                p.titulo.contains(q, ignoreCase = true) ||
                p.descripcion.contains(q, ignoreCase = true) ||
                p.nombreTienda.contains(q, ignoreCase = true)
            matchCat && matchZona && matchQuery
        }

    val totalUnidadesCarrito: Int
        get() = cartItems.sumOf { it.cantidad }

    val carritoAgrupadoPorTienda: Map<String, List<CartItemLocal>>
        get() = cartItems.groupBy { it.producto.emprendimientoId }
}

class CatalogViewModel(
    private val repository: CatalogRepository = CatalogRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    init {
        cargarCatalogo()
    }

    fun cargarCatalogo() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val (cats, tiendas, prods) = withContext(Dispatchers.IO) {
                    val c = repository.getCategoriasActivas()
                    val t = repository.getEmprendimientosAprobados().associateBy { it.id }
                    val p = repository.getProductosActivos()
                    Triple(c, t, p)
                }
                _state.update {
                    it.copy(
                        loading = false,
                        categorias = cats,
                        tiendas = tiendas,
                        productos = prods
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = "No se pudo cargar el catálogo: ${e.message}")
                }
            }
        }
    }

    fun onQueryChange(v: String) = _state.update { it.copy(query = v) }
    fun onSelectCategoria(catId: String?) = _state.update { it.copy(categoriaSeleccionadaId = catId) }
    fun onSelectZona(zona: String?) = _state.update { it.copy(zonaSeleccionada = zona) }

    fun agregarAlCarrito(producto: Producto): String {
        if (producto.stock <= 0) return "Este producto está agotado."
        var mensaje = ""
        _state.update { s ->
            val existente = s.cartItems.firstOrNull { it.producto.id == producto.id }
            val nuevaLista = if (existente != null) {
                if (existente.cantidad >= producto.stock) {
                    mensaje = "Alcanzaste el stock máximo disponible (${producto.stock})."
                    s.cartItems
                } else {
                    mensaje = "Se sumó otra unidad de ${producto.titulo}."
                    s.cartItems.map {
                        if (it.producto.id == producto.id) it.copy(cantidad = it.cantidad + 1) else it
                    }
                }
            } else {
                mensaje = "Agregado al carrito: ${producto.titulo}"
                s.cartItems + CartItemLocal(producto = producto, cantidad = 1)
            }
            s.copy(cartItems = nuevaLista)
        }
        return mensaje
    }

    fun cambiarCantidadCarrito(productoId: String, delta: Int) {
        _state.update { s ->
            val nuevaLista = s.cartItems.mapNotNull { item ->
                if (item.producto.id != productoId) {
                    item
                } else {
                    val nextQty = item.cantidad + delta
                    when {
                        nextQty <= 0 -> null
                        nextQty > item.producto.stock -> item
                        else -> item.copy(cantidad = nextQty)
                    }
                }
            }
            s.copy(cartItems = nuevaLista)
        }
    }

    fun eliminarDeCarrito(productoId: String) {
        _state.update { s ->
            s.copy(cartItems = s.cartItems.filterNot { it.producto.id == productoId })
        }
    }

    fun vaciarTiendaDeCarrito(emprendimientoId: String) {
        _state.update { s ->
            s.copy(cartItems = s.cartItems.filterNot { it.producto.emprendimientoId == emprendimientoId })
        }
    }

    fun limpiarMensajes() = _state.update { it.copy(error = null, infoBanner = null) }
}
