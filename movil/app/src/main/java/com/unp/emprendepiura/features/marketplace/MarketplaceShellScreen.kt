package com.unp.emprendepiura.features.marketplace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.core.theme.appleSubRouteTransition
import com.unp.emprendepiura.core.theme.appleTabTransition
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.model.RolUsuario
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
import com.unp.emprendepiura.features.chat.PerfilChatObjetivo
import com.unp.emprendepiura.features.chat.PerfilContactoChatScreen
import com.unp.emprendepiura.features.checkout.CartScreen
import com.unp.emprendepiura.features.checkout.MisPedidosScreen
import com.unp.emprendepiura.features.checkout.OrdersViewModel
import com.unp.emprendepiura.features.checkout.PedidosSubTab
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
    data class PerfilContactoChat(
        val conversacion: Conversacion?,
        val tiendaId: String?,
        val compradorId: String?,
        val nombreFallback: String,
        val volverA: MarketSubRoute
    ) : MarketSubRoute()
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
    var pedidosSubTab by remember { mutableStateOf(PedidosSubTab.MIS_COMPRAS) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> chatVm.actualizarAppEnPrimerPlano(true)
                Lifecycle.Event.ON_PAUSE -> chatVm.actualizarAppEnPrimerPlano(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tiendaAprobada = remember(usuario?.id, storeState.miTienda) {
        if (usuario == null) null
        else storeState.miTienda?.takeIf { it.estado == EstadoEmprendimiento.APROBADO }
    }

    val tiendasBloqueadasComprador = remember(
        usuario?.id,
        chatState.conversaciones,
        chatState.conversacionActiva
    ) {
        chatState.tiendasBloqueadasParaComprador(usuario?.id.orEmpty())
    }

    LaunchedEffect(tiendasBloqueadasComprador) {
        catalogVm.actualizarTiendasBloqueadas(tiendasBloqueadasComprador)
    }

    LaunchedEffect(usuario?.id) {
        if (usuario != null && usuario.id.isNotBlank()) {
            storeVm.iniciarObservacionMiTienda(usuario.id) {
                onRefrescarUsuario()
            }
        } else {
            storeVm.limpiarEstadoSesion()
            adminVm.detenerObservacionAdmin()
            chatVm.detenerObservacionYSesion()
            ordersVm.limpiarEstadoSesion()
            catalogVm.actualizarTiendasBloqueadas(emptySet())
            pedidosSubTab = PedidosSubTab.MIS_COMPRAS
            subRoute = MarketSubRoute.Tabs
            currentTab = MarketTab.EXPLORAR
        }
    }

    LaunchedEffect(tiendaAprobada?.id) {
        if (tiendaAprobada != null) {
            pedidosSubTab = PedidosSubTab.POR_ENTREGAR
        }
    }

    LaunchedEffect(usuario?.id, storeState.miTienda?.id, tiendaAprobada?.id) {
        if (usuario != null && usuario.id.isNotBlank()) {
            val tiendaId = storeState.miTienda?.id
            chatVm.iniciarObservacionBandeja(context, usuario.id, tiendaId)
            ordersVm.iniciarObservacionOrdenesRealtime(
                context = context,
                compradorId = usuario.id,
                miEmprendimientoId = tiendaAprobada?.id
            ) {
                catalogVm.cargarCatalogo(silencioso = true)
            }
        }
    }

    LaunchedEffect(chatState.salaEliminadaPorContraparte) {
        if (chatState.salaEliminadaPorContraparte &&
            (subRoute == MarketSubRoute.SalaChat || subRoute is MarketSubRoute.PerfilContactoChat)
        ) {
            subRoute = MarketSubRoute.Tabs
            chatVm.consumirEventoSalaEliminada()
        }
    }

    BackHandler(enabled = subRoute != MarketSubRoute.Tabs) {
        when (val current = subRoute) {
            is MarketSubRoute.PerfilContactoChat -> subRoute = current.volverA
            MarketSubRoute.SalaChat -> {
                chatVm.cerrarSala(usuario?.id)
                subRoute = MarketSubRoute.Tabs
            }
            MarketSubRoute.PanelAdmin -> {
                adminVm.detenerObservacionAdmin()
                subRoute = MarketSubRoute.Tabs
            }
            else -> subRoute = MarketSubRoute.Tabs
        }
    }

    val bannerError = ordersState.error ?: chatState.error ?: storeState.error ?: adminState.error ?: catalogState.error
    val bannerExito = ordersState.exito ?: chatState.exito ?: storeState.exito ?: adminState.exito ?: catalogState.infoBanner

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
                                            when (tab) {
                                                MarketTab.CARRITO -> {
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
                                                }
                                                MarketTab.CHATS -> {
                                                    BadgedBox(
                                                        badge = {
                                                            androidx.compose.animation.AnimatedVisibility(
                                                                visible = usuario != null && chatState.totalNoLeidos > 0,
                                                                enter = scaleIn(appleBouncySpring()) + fadeIn(),
                                                                exit = scaleOut() + fadeOut()
                                                            ) {
                                                                Badge {
                                                                    Text(
                                                                        if (chatState.totalNoLeidos > 99) "99+"
                                                                        else "${chatState.totalNoLeidos}"
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(icon, contentDescription = tab.label)
                                                    }
                                                }
                                                MarketTab.PEDIDOS -> {
                                                    BadgedBox(
                                                        badge = {
                                                            androidx.compose.animation.AnimatedVisibility(
                                                                visible = tiendaAprobada != null && ordersState.entregasPendientesCount > 0,
                                                                enter = scaleIn(appleBouncySpring()) + fadeIn(),
                                                                exit = scaleOut() + fadeOut()
                                                            ) {
                                                                Badge { Text("${ordersState.entregasPendientesCount}") }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(icon, contentDescription = tab.label)
                                                    }
                                                }
                                                else -> Icon(icon, contentDescription = tab.label)
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
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            label = "marketplaceTabs"
                        ) { tab ->
                            when (tab) {
                                MarketTab.EXPLORAR -> ExplorarScreen(
                                    state = catalogState,
                                    onQueryChange = catalogVm::onQueryChange,
                                    onSelectCategoria = catalogVm::onSelectCategoria,
                                    onSelectZona = catalogVm::onSelectZona,
                                    onRefresh = { catalogVm.cargarCatalogo() },
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
                                            miUsuarioId = usuario.id,
                                            darkTheme = darkTheme,
                                            onRefresh = { chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id) },
                                            onAbrirConversacion = { conv ->
                                                chatVm.abrirSala(conv, usuario.id)
                                                subRoute = MarketSubRoute.SalaChat
                                            },
                                            onAbrirPerfilContacto = { conv ->
                                                val soyComprador = conv.compradorId == usuario.id
                                                subRoute = MarketSubRoute.PerfilContactoChat(
                                                    conversacion = conv,
                                                    tiendaId = if (soyComprador) conv.emprendimientoId else null,
                                                    compradorId = if (soyComprador) null else conv.compradorId,
                                                    nombreFallback = conv.nombreContraparte,
                                                    volverA = MarketSubRoute.Tabs
                                                )
                                            },
                                            onBloquearConversacion = { chatVm.bloquearConversacion(it, usuario.id) },
                                            onDesbloquearConversacion = { chatVm.desbloquearConversacion(it, usuario.id) },
                                            onEliminarConversacion = { id ->
                                                chatVm.eliminarConversacion(id) {
                                                    chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id, silencioso = true)
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
                                                    pedidosSubTab = PedidosSubTab.MIS_COMPRAS
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
                                            ordenesCompra = ordersState.misOrdenes,
                                            ordenesVenta = if (tiendaAprobada != null) ordersState.ordenesRecibidas else emptyList(),
                                            tieneTiendaAprobada = tiendaAprobada != null,
                                            subTabActual = pedidosSubTab,
                                            onSelectSubTab = { pedidosSubTab = it },
                                            loading = ordersState.loading,
                                            onRefresh = {
                                                ordersVm.cargarMisOrdenes(usuario.id)
                                                tiendaAprobada?.id?.let { ordersVm.cargarOrdenesRecibidas(it) }
                                            },
                                            onChatearCompra = { orden ->
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
                                                    nuevoEstado = EstadoOrden.CANCELADO,
                                                    compradorIdRefresh = usuario.id
                                                )
                                                catalogVm.cargarCatalogo()
                                            },
                                            onConfirmarRecepcion = { orden ->
                                                ordersVm.confirmarRecepcionComprador(orden, usuario.id) {
                                                    catalogVm.cargarCatalogo()
                                                }
                                            },
                                            onChatearVenta = { orden ->
                                                chatVm.abrirChatConComprador(
                                                    miUsuarioId = usuario.id,
                                                    compradorId = orden.compradorId,
                                                    emprendimientoId = orden.emprendimientoId,
                                                    nombreComprador = orden.nombreComprador,
                                                    ordenId = orden.id,
                                                    onAbierto = { subRoute = MarketSubRoute.SalaChat }
                                                )
                                            },
                                            onAvanzarOrdenVenta = { orden, nuevoEstado ->
                                                val tiendaId = tiendaAprobada?.id
                                                ordersVm.cambiarEstadoOrden(
                                                    ordenId = orden.id,
                                                    nuevoEstado = nuevoEstado,
                                                    emprendimientoIdRefresh = tiendaId
                                                )
                                                catalogVm.cargarCatalogo()
                                            },
                                            onConfirmarEntregaConPin = { orden, pin ->
                                                val tiendaId = tiendaAprobada?.id
                                                ordersVm.confirmarEntregaConPin(
                                                    orden = orden,
                                                    pinIngresado = pin,
                                                    emprendimientoIdRefresh = tiendaId
                                                ) {
                                                    catalogVm.cargarCatalogo()
                                                }
                                            }
                                        )
                                    }
                                }

                                MarketTab.PERFIL -> PerfilMarketplaceScreen(
                                    usuario = usuario,
                                    miTienda = if (usuario == null) null else storeState.miTienda,
                                    entregasPendientesCount = ordersState.entregasPendientesCount,
                                    darkTheme = darkTheme,
                                    onToggleTheme = onToggleTheme,
                                    onAbrirPedidosPorEntregar = {
                                        tiendaAprobada?.id?.let { ordersVm.cargarOrdenesRecibidas(it) }
                                        pedidosSubTab = PedidosSubTab.POR_ENTREGAR
                                        currentTab = MarketTab.PEDIDOS
                                    },
                                    onAbrirMiTienda = {
                                        tiendaAprobada?.id?.let { ordersVm.cargarOrdenesRecibidas(it) }
                                        subRoute = MarketSubRoute.MiTienda
                                    },
                                    onSolicitarTienda = { subRoute = MarketSubRoute.SolicitarTienda },
                                    onAbrirPanelAdmin = {
                                        adminVm.iniciarObservacionAdmin()
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
                    val p = catalogState.productos.firstOrNull { it.id == route.producto.id } ?: route.producto
                    val tiendaProd = catalogState.tiendas[p.emprendimientoId]
                    val tiendaBloqueada = p.emprendimientoId in tiendasBloqueadasComprador
                    ProductoDetalleScreen(
                        producto = p,
                        tienda = tiendaProd,
                        esPropioProducto = usuario != null && storeState.miTienda?.id == p.emprendimientoId,
                        tiendaBloqueada = tiendaBloqueada,
                        onVolver = { subRoute = MarketSubRoute.Tabs },
                        onAgregarCarrito = { catalogVm.agregarAlCarrito(it) },
                        onIrAlCarrito = {
                            currentTab = MarketTab.CARRITO
                            subRoute = MarketSubRoute.Tabs
                        },
                        onAbrirPerfilTienda = {
                            subRoute = MarketSubRoute.PerfilContactoChat(
                                conversacion = null,
                                tiendaId = p.emprendimientoId,
                                compradorId = null,
                                nombreFallback = tiendaProd?.nombreTienda ?: p.nombreTienda,
                                volverA = route
                            )
                        },
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
                            darkTheme = darkTheme,
                            loading = chatState.loadingMensajes,
                            enviando = chatState.enviando,
                            enviandoAdjunto = chatState.enviandoAdjunto,
                            etiquetaAdjuntoEnProgreso = chatState.etiquetaAdjuntoEnProgreso,
                            contraparteEnLinea = chatState.contraparteEnLinea,
                            contraparteEscribiendo = chatState.contraparteEscribiendo,
                            onNotificarEscritura = { escribiendo ->
                                chatVm.notificarEscritura(usuario.id, escribiendo)
                            },
                            onAbrirPerfilContacto = {
                                val soyComprador = conv.compradorId == usuario.id
                                subRoute = MarketSubRoute.PerfilContactoChat(
                                    conversacion = conv,
                                    tiendaId = if (soyComprador) conv.emprendimientoId else null,
                                    compradorId = if (soyComprador) null else conv.compradorId,
                                    nombreFallback = conv.nombreContraparte,
                                    volverA = MarketSubRoute.SalaChat
                                )
                            },
                            onEnviarTexto = { textoMsg, reply ->
                                chatVm.enviarMensaje(usuario.id, textoMsg, reply)
                            },
                            onEnviarImagen = { uri, reply ->
                                chatVm.enviarImagenAdjunta(context, usuario.id, uri, reply)
                            },
                            onEnviarAudio = { bytes, dur, reply ->
                                chatVm.enviarAudioAdjunto(usuario.id, bytes, dur, reply)
                            },
                            onEnviarDocumento = { uri, reply ->
                                chatVm.enviarDocumentoAdjunto(context, usuario.id, uri, reply)
                            },
                            onEnviarUbicacion = { reply ->
                                chatVm.enviarUbicacionAdjunta(context, usuario.id, reply)
                            },
                            onEnviarContacto = { nom, tel, reply ->
                                chatVm.enviarContactoAdjunto(usuario.id, nom, tel, reply)
                            },
                            onEliminarMensajeParaMi = { msg ->
                                chatVm.eliminarMensajeParaMi(msg, usuario.id)
                            },
                            onEliminarMensajeParaTodos = { msg ->
                                chatVm.eliminarMensajeParaTodos(msg, usuario.id)
                            },
                            onMostrarAviso = chatVm::mostrarAvisoChat,
                            onBloquear = { chatVm.bloquearConversacion(conv.id, usuario.id) },
                            onDesbloquear = { chatVm.desbloquearConversacion(conv.id, usuario.id) },
                            onEliminarConversacion = {
                                chatVm.eliminarConversacion(conv.id) {
                                    chatVm.cerrarSala(usuario.id)
                                    chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id, silencioso = true)
                                    subRoute = MarketSubRoute.Tabs
                                }
                            },
                            onVolver = {
                                chatVm.cerrarSala(usuario.id)
                                chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id, silencioso = true)
                                subRoute = MarketSubRoute.Tabs
                            }
                        )
                    }
                }

                is MarketSubRoute.PerfilContactoChat -> {
                    val convFresca = route.conversacion?.let { c ->
                        chatState.conversacionActiva?.takeIf { it.id == c.id }
                            ?: chatState.conversaciones.firstOrNull { it.id == c.id }
                            ?: c
                    } ?: if (usuario != null && route.tiendaId != null) {
                        chatState.conversaciones.firstOrNull {
                            it.compradorId == usuario.id && it.emprendimientoId == route.tiendaId
                        }
                    } else null
                    val objetivo: PerfilChatObjetivo = if (route.tiendaId != null) {
                        val tienda = chatState.tiendaContraparte?.takeIf { it.id == route.tiendaId }
                            ?: catalogState.tiendas[route.tiendaId]
                        val prods = if (convFresca?.estaBloqueada == true) {
                            emptyList()
                        } else {
                            catalogState.productos.filter { it.emprendimientoId == route.tiendaId }
                        }
                        PerfilChatObjetivo.EmprendimientoPerfil(
                            tienda = tienda,
                            nombreFallback = route.nombreFallback,
                            productosTienda = prods
                        )
                    } else {
                        val comprador = chatState.compradorContraparte?.takeIf { it.id == route.compradorId }
                            ?: route.compradorId?.let { chatState.usuariosCache[it] }
                        val entregas = ordersState.ordenesRecibidas.count {
                            it.compradorId == route.compradorId && it.estado == EstadoOrden.ENTREGADO
                        }
                        PerfilChatObjetivo.CompradorPerfil(
                            comprador = comprador,
                            nombreFallback = route.nombreFallback,
                            entregasConTienda = entregas
                        )
                    }

                    PerfilContactoChatScreen(
                        objetivo = objetivo,
                        conversacion = convFresca,
                        miUsuarioId = usuario?.id.orEmpty(),
                        darkTheme = darkTheme,
                        contraparteEnLinea = route.volverA == MarketSubRoute.SalaChat && chatState.contraparteEnLinea,
                        onVolver = { subRoute = route.volverA },
                        onIrAlChat = {
                            if (usuario == null) {
                                onIrAAuth()
                            } else if (convFresca != null) {
                                if (chatState.conversacionActiva?.id != convFresca.id) {
                                    chatVm.abrirSala(convFresca, usuario.id)
                                }
                                subRoute = MarketSubRoute.SalaChat
                            } else if (route.tiendaId != null) {
                                chatVm.abrirChatConTienda(
                                    compradorId = usuario.id,
                                    emprendimientoId = route.tiendaId,
                                    nombreTienda = route.nombreFallback,
                                    onAbierto = { subRoute = MarketSubRoute.SalaChat }
                                )
                            }
                        },
                        onAbrirProducto = { prod -> subRoute = MarketSubRoute.DetalleProducto(prod) },
                        onBloquear = {
                            if (convFresca != null && usuario != null) {
                                chatVm.bloquearConversacion(convFresca.id, usuario.id)
                            }
                        },
                        onDesbloquear = {
                            if (convFresca != null && usuario != null) {
                                chatVm.desbloquearConversacion(convFresca.id, usuario.id)
                            }
                        },
                        onEliminarConversacion = {
                            if (convFresca != null && usuario != null) {
                                chatVm.eliminarConversacion(convFresca.id) {
                                    chatVm.cerrarSala(usuario.id)
                                    chatVm.cargarConversaciones(usuario.id, storeState.miTienda?.id, silencioso = true)
                                    subRoute = MarketSubRoute.Tabs
                                }
                            }
                        }
                    )
                }

                MarketSubRoute.SolicitarTienda -> if (usuario != null && usuario.rol != RolUsuario.ADMIN) {
                    SolicitarTiendaScreen(
                        categorias = catalogState.categorias,
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
                    entregasPendientesCount = ordersState.entregasPendientesCount,
                    categorias = catalogState.categorias,
                    onVolver = { subRoute = MarketSubRoute.Tabs },
                    onIrAPedidosPorEntregar = {
                        tiendaAprobada?.id?.let { ordersVm.cargarOrdenesRecibidas(it) }
                        pedidosSubTab = PedidosSubTab.POR_ENTREGAR
                        currentTab = MarketTab.PEDIDOS
                        subRoute = MarketSubRoute.Tabs
                    },
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
                    }
                )

                MarketSubRoute.PanelAdmin -> AdminPanelScreen(
                    state = adminState,
                    miAdminUsuarioId = usuario?.id.orEmpty(),
                    onRefresh = { adminVm.cargarTodo() },
                    onToggleUsuarioActivo = { u ->
                        adminVm.toggleUsuarioActivo(u, usuario?.id.orEmpty())
                    },
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
                    onVolver = {
                        adminVm.detenerObservacionAdmin()
                        subRoute = MarketSubRoute.Tabs
                    }
                )
            }
        }

        SwipeableFeedbackBanner(
            errorMessage = bannerError,
            successMessage = if (bannerError != null) null else bannerExito,
            onClearMessages = {
                ordersVm.limpiarMensajes()
                chatVm.limpiarMensajes()
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
