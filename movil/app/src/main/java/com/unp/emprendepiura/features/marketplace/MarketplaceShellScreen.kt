package com.unp.emprendepiura.features.marketplace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.core.theme.appleSubRouteTransition
import com.unp.emprendepiura.core.theme.appleTabTransition
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.model.Usuario
import com.unp.emprendepiura.features.admin.AdminPanelScreen
import com.unp.emprendepiura.features.admin.AdminViewModel
import com.unp.emprendepiura.features.auth.ui.components.SwipeableFeedbackBanner
import com.unp.emprendepiura.features.catalog.CatalogViewModel
import com.unp.emprendepiura.features.catalog.ExplorarScreen
import com.unp.emprendepiura.features.catalog.ProductoDetalleScreen
import com.unp.emprendepiura.features.chat.ChatRoomScreen
import com.unp.emprendepiura.features.chat.ChatViewModel
import com.unp.emprendepiura.features.chat.ChatsListScreen
import com.unp.emprendepiura.features.checkout.CartScreen
import com.unp.emprendepiura.features.checkout.MisPedidosScreen
import com.unp.emprendepiura.features.checkout.OrdersViewModel
import com.unp.emprendepiura.features.store.MiTiendaScreen
import com.unp.emprendepiura.features.store.SolicitarTiendaScreen
import com.unp.emprendepiura.features.store.StoreViewModel

private enum class MarketTab(val label: String) {
    EXPLORAR("Explorar"),
    CHATS("Chats"),
    CARRITO("Carrito"),
    PEDIDOS("Pedidos"),
    PERFIL("Perfil")
}

private sealed class MarketSubRoute {
    data object Tabs : MarketSubRoute()
    data class DetalleProducto(val producto: Producto) : MarketSubRoute()
    data object SalaChat : MarketSubRoute()
    data object SolicitarTienda : MarketSubRoute()
    data object MiTienda : MarketSubRoute()
    data object PanelAdmin : MarketSubRoute()
}

@Composable
fun MarketplaceShellScreen(
    usuario: Usuario?,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onIrAAuth: () -> Unit,
    onIrARegistro: () -> Unit,
    onEditarPerfilBiometria: () -> Unit,
    onRecapturarHuella: () -> Unit,
    onRecapturarRostro: () -> Unit,
    onRefrescarUsuario: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val catalogVm: CatalogViewModel = viewModel()
    val ordersVm: OrdersViewModel = viewModel()
    val chatVm: ChatViewModel = viewModel()
    val storeVm: StoreViewModel = viewModel()
    val adminVm: AdminViewModel = viewModel()

    val catalogState by catalogVm.state.collectAsState()
    val ordersState by ordersVm.state.collectAsState()
    val chatState by chatVm.state.collectAsState()
    val storeState by storeVm.state.collectAsState()
    val adminState by adminVm.state.collectAsState()

    var currentTab by remember { mutableStateOf(MarketTab.EXPLORAR) }
    var subRoute by remember { mutableStateOf<MarketSubRoute>(MarketSubRoute.Tabs) }

    // Cargar datos dependientes del usuario autenticado
    LaunchedEffect(usuario?.id) {
        if (usuario != null && usuario.id.isNotBlank()) {
            storeVm.cargarMiTienda(usuario.id)
            ordersVm.cargarMisOrdenes(usuario.id)
        }
    }

    LaunchedEffect(usuario?.id, storeState.miTienda?.id) {
        if (usuario != null && usuario.id.isNotBlank()) {
            chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id)
            storeState.miTienda?.id?.let { ordersVm.cargarOrdenesRecibidas(it) }
        }
    }

    BackHandler(enabled = subRoute != MarketSubRoute.Tabs) {
        if (subRoute == MarketSubRoute.SalaChat) chatVm.cerrarSala()
        subRoute = MarketSubRoute.Tabs
    }

    // Banner unificado de feedback para todas las pantallas del Marketplace
    val bannerError = ordersState.error ?: chatState.error ?: storeState.error ?: adminState.error ?: catalogState.error
    val bannerExito = ordersState.exito ?: storeState.exito ?: adminState.exito ?: catalogState.infoBanner

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = subRoute,
            transitionSpec = {
                appleSubRouteTransition(esRegresoATabs = targetState == MarketSubRoute.Tabs)
            },
            label = "marketplaceSubRoute"
        ) { route ->
            when (route) {
                MarketSubRoute.Tabs -> {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                MarketTab.entries.forEach { tab ->
                                    val icon = when (tab) {
                                        MarketTab.EXPLORAR -> Icons.Default.Home
                                        MarketTab.CHATS -> Icons.AutoMirrored.Filled.Chat
                                        MarketTab.CARRITO -> Icons.Default.ShoppingCart
                                        MarketTab.PEDIDOS -> Icons.AutoMirrored.Filled.ReceiptLong
                                        MarketTab.PERFIL -> Icons.Default.Person
                                    }
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            if (tab == MarketTab.CARRITO) {
                                                BadgedBox(
                                                    badge = {
                                                        androidx.compose.animation.AnimatedVisibility(
                                                            visible = catalogState.totalUnidadesCarrito > 0,
                                                            enter = scaleIn(appleBouncySpring()) + fadeIn(),
                                                            exit = scaleOut() + fadeOut()
                                                        ) {
                                                            Badge { Text("${catalogState.totalUnidadesCarrito}") }
                                                        }
                                                    }
                                                ) {
                                                    Icon(icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { appleTabTransition() },
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            label = "marketplaceTabs"
                        ) { tab ->
                            when (tab) {
                                MarketTab.EXPLORAR -> ExplorarScreen(
                                    state = catalogState,
                                    onQueryChange = catalogVm::onQueryChange,
                                    onSelectCategoria = catalogVm::onSelectCategoria,
                                    onSelectZona = catalogVm::onSelectZona,
                                    onRefresh = catalogVm::cargarCatalogo,
                                    onAbrirProducto = { prod -> subRoute = MarketSubRoute.DetalleProducto(prod) }
                                )

                                MarketTab.CHATS -> {
                                    if (usuario == null) {
                                        InvitadoGateBox(
                                            titulo = "Inicia sesión para chatear",
                                            onIrALogin = onIrAAuth
                                        )
                                    } else {
                                        ChatsListScreen(
                                            state = chatState,
                                            tiendas = catalogState.tiendas,
                                            onRefresh = { chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id) },
                                            onAbrirConversacion = { conv ->
                                                chatVm.abrirSala(conv)
                                                subRoute = MarketSubRoute.SalaChat
                                            },
                                            onBloquearConversacion = { chatVm.bloquearConversacion(it) },
                                            onDesbloquearConversacion = { chatVm.desbloquearConversacion(it) },
                                            onEliminarConversacion = { id ->
                                                chatVm.eliminarConversacion(id) {
                                                    chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id)
                                                }
                                            }
                                        )
                                    }
                                }

                                MarketTab.CARRITO -> CartScreen(
                                    cartAgrupado = catalogState.carritoAgrupadoPorTienda,
                                    tiendas = catalogState.tiendas,
                                    procesandoCheckout = ordersState.procesandoCheckout,
                                    onChangeQty = catalogVm::cambiarCantidadCarrito,
                                    onRemoveItem = catalogVm::eliminarDeCarrito,
                                    onConfirmarCheckout = { tiendaId, entrega, punto, pago, compUri, opCode, notas, items ->
                                        if (usuario == null) {
                                            onIrAAuth()
                                        } else {
                                            ordersVm.confirmarPedidoTienda(
                                                context = context,
                                                compradorId = usuario.id,
                                                emprendimientoId = tiendaId,
                                                tipoEntrega = entrega,
                                                direccionOPunto = punto,
                                                metodoPago = pago,
                                                comprobanteUri = compUri,
                                                codigoOperacion = opCode,
                                                notas = notas,
                                                items = items,
                                                onOrdenCreada = {
                                                    catalogVm.vaciarTiendaDeCarrito(tiendaId)
                                                    catalogVm.cargarCatalogo()
                                                    currentTab = MarketTab.PEDIDOS
                                                }
                                            )
                                        }
                                    },
                                    onIrAExplorar = { currentTab = MarketTab.EXPLORAR }
                                )

                                MarketTab.PEDIDOS -> {
                                    if (usuario == null) {
                                        InvitadoGateBox(
                                            titulo = "Inicia sesión para ver tus pedidos",
                                            onIrALogin = onIrAAuth
                                        )
                                    } else {
                                        MisPedidosScreen(
                                            ordenes = ordersState.misOrdenes,
                                            loading = ordersState.loading,
                                            onRefresh = { ordersVm.cargarMisOrdenes(usuario.id) },
                                            onChatearOrden = { orden ->
                                                chatVm.abrirChatConTienda(
                                                    compradorId = usuario.id,
                                                    emprendimientoId = orden.emprendimientoId,
                                                    nombreTienda = orden.nombreTienda,
                                                    ordenId = orden.id,
                                                    onAbierto = { subRoute = MarketSubRoute.SalaChat }
                                                )
                                            },
                                            onCancelarOrden = { orden ->
                                                ordersVm.cambiarEstadoOrden(
                                                    ordenId = orden.id,
                                                    nuevoEstado = com.unp.emprendepiura.domain.model.EstadoOrden.CANCELADO,
                                                    compradorIdRefresh = usuario.id
                                                )
                                                catalogVm.cargarCatalogo()
                                            },
                                            onConfirmarRecepcion = { orden ->
                                                ordersVm.confirmarRecepcionComprador(orden, usuario.id) {
                                                    catalogVm.cargarCatalogo()
                                                }
                                            }
                                        )
                                    }
                                }

                                MarketTab.PERFIL -> PerfilMarketplaceScreen(
                                    usuario = usuario,
                                    miTienda = if (usuario == null) null else storeState.miTienda,
                                    darkTheme = darkTheme,
                                    onToggleTheme = onToggleTheme,
                                    onAbrirMiTienda = {
                                        storeState.miTienda?.id?.let { ordersVm.cargarOrdenesRecibidas(it) }
                                        subRoute = MarketSubRoute.MiTienda
                                    },
                                    onSolicitarTienda = { subRoute = MarketSubRoute.SolicitarTienda },
                                    onAbrirPanelAdmin = {
                                        adminVm.cargarTodo()
                                        subRoute = MarketSubRoute.PanelAdmin
                                    },
                                    onEditarDatosPerfil = onEditarPerfilBiometria,
                                    onRecapturarHuella = onRecapturarHuella,
                                    onRecapturarRostro = onRecapturarRostro,
                                    onIrALogin = onIrAAuth,
                                    onIrARegistro = onIrARegistro,
                                    onCerrarSesion = onCerrarSesion
                                )
                            }
                        }
                    }
                }

                is MarketSubRoute.DetalleProducto -> {
                    val p = route.producto
                    ProductoDetalleScreen(
                        producto = p,
                        tienda = catalogState.tiendas[p.emprendimientoId],
                        esPropioProducto = usuario != null && storeState.miTienda?.id == p.emprendimientoId,
                        onVolver = { subRoute = MarketSubRoute.Tabs },
                        onAgregarCarrito = { catalogVm.agregarAlCarrito(it) },
                        onChatearConTienda = { prod ->
                            if (usuario == null) {
                                onIrAAuth()
                            } else {
                                chatVm.abrirChatConTienda(
                                    compradorId = usuario.id,
                                    emprendimientoId = prod.emprendimientoId,
                                    nombreTienda = prod.nombreTienda,
                                    productoId = prod.id,
                                    onAbierto = { subRoute = MarketSubRoute.SalaChat }
                                )
                            }
                        }
                    )
                }

                MarketSubRoute.SalaChat -> {
                    val conv = chatState.conversacionActiva
                    if (conv != null && usuario != null) {
                        ChatRoomScreen(
                            conversacion = conv,
                            mensajes = chatState.mensajes,
                            tiendaContraparte = chatState.tiendaContraparte ?: catalogState.tiendas[conv.emprendimientoId],
                            miUsuarioId = usuario.id,
                            loading = chatState.loadingMensajes,
                            enviando = chatState.enviando,
                            contraparteEscribiendo = chatState.contraparteEscribiendo,
                            estaBloqueado = chatState.conversacionesBloqueadas.contains(conv.id),
                            onEnviar = { chatVm.enviarMensaje(usuario.id, it) },
                            onBloquear = { chatVm.bloquearConversacion(conv.id) },
                            onDesbloquear = { chatVm.desbloquearConversacion(conv.id) },
                            onEliminarConversacion = {
                                chatVm.eliminarConversacion(conv.id) {
                                    chatVm.cerrarSala()
                                    chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id)
                                    subRoute = MarketSubRoute.Tabs
                                }
                            },
                            onVolver = {
                                chatVm.cerrarSala()
                                chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id)
                                subRoute = MarketSubRoute.Tabs
                            }
                        )
                    }
                }

                MarketSubRoute.SolicitarTienda -> if (usuario != null) {
                    SolicitarTiendaScreen(
                        categorias = catalogState.categorias,
                        telefonoInicial = usuario.telefono,
                        zonaInicial = usuario.zonaPiura,
                        saving = storeState.saving,
                        onEnviarSolicitud = { nom, desc, cat, zona, punto, tel, yape ->
                            storeVm.enviarSolicitudTienda(
                                usuarioId = usuario.id,
                                nombreTienda = nom,
                                descripcion = desc,
                                categoriaPrincipal = cat,
                                zonaPiura = zona,
                                puntoEncuentro = punto,
                                telefono = tel,
                                numeroYapePlin = yape,
                                onGuardado = { subRoute = MarketSubRoute.Tabs }
                            )
                        },
                        onVolver = { subRoute = MarketSubRoute.Tabs }
                    )
                }

                MarketSubRoute.MiTienda -> MiTiendaScreen(
                    storeState = storeState,
                    ordenesRecibidas = ordersState.ordenesRecibidas,
                    categorias = catalogState.categorias,
                    onVolver = { subRoute = MarketSubRoute.Tabs },
                    onGuardarProducto = { prodExist, t, d, p, s, catId, z, uri, onDone ->
                        val tiendaId = storeState.miTienda?.id ?: return@MiTiendaScreen
                        storeVm.guardarProducto(context, prodExist, tiendaId, t, d, p, s, catId, z, uri) {
                            catalogVm.cargarCatalogo()
                            onDone()
                        }
                    },
                    onToggleEstadoProducto = { prod ->
                        val tiendaId = storeState.miTienda?.id ?: return@MiTiendaScreen
                        storeVm.toggleEstadoProducto(prod, tiendaId)
                        catalogVm.cargarCatalogo()
                    },
                    onAvanzarOrden = { orden, nuevoEstado ->
                        val tiendaId = storeState.miTienda?.id
                        ordersVm.cambiarEstadoOrden(orden.id, nuevoEstado, emprendimientoIdRefresh = tiendaId)
                        catalogVm.cargarCatalogo()
                    },
                    onConfirmarEntregaConPin = { orden, pin ->
                        val tiendaId = storeState.miTienda?.id
                        ordersVm.confirmarEntregaConPin(orden, pin, emprendimientoIdRefresh = tiendaId) {
                            catalogVm.cargarCatalogo()
                        }
                    }
                )

                MarketSubRoute.PanelAdmin -> AdminPanelScreen(
                    state = adminState,
                    onRefresh = adminVm::cargarTodo,
                    onResolverTienda = { tienda, estado ->
                        adminVm.resolverEmprendimiento(tienda, estado) {
                            catalogVm.cargarCatalogo()
                            onRefrescarUsuario()
                            if (usuario != null) storeVm.cargarMiTienda(usuario.id)
                        }
                    },
                    onCrearCategoria = { nom ->
                        adminVm.crearCategoria(nom) { catalogVm.cargarCatalogo() }
                    },
                    onToggleCategoria = { cat ->
                        adminVm.toggleCategoria(cat) { catalogVm.cargarCatalogo() }
                    },
                    onModerarProducto = { prod ->
                        adminVm.moderarProducto(prod) { catalogVm.cargarCatalogo() }
                    },
                    onVolver = { subRoute = MarketSubRoute.Tabs }
                )
            }
        }

        SwipeableFeedbackBanner(
            errorMessage = bannerError,
            successMessage = bannerExito,
            onClearMessages = {
                ordersVm.limpiarMensajes()
                storeVm.limpiarMensajes()
                adminVm.limpiarMensajes()
                catalogVm.limpiarMensajes()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun InvitadoGateBox(titulo: String, onIrALogin: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        com.unp.emprendepiura.core.ui.EmptyStateBox(
            icon = Icons.Default.Person,
            titulo = titulo,
            subtitulo = "Ingresa con tu DNI usando huella, reconocimiento facial o contraseña.",
            accionTexto = "Ir a Iniciar Sesión",
            onAccionClick = onIrALogin
        )
    }
}
