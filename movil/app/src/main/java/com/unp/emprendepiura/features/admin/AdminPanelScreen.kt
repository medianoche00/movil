package com.unp.emprendepiura.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    state: AdminUiState,
    miAdminUsuarioId: String,
    onRefresh: () -> Unit,
    onToggleUsuarioActivo: (Usuario) -> Unit,
    onResolverTienda: (Emprendimiento, EstadoEmprendimiento) -> Unit,
    onCrearCategoria: (String) -> Unit,
    onToggleCategoria: (Categoria) -> Unit,
    onModerarProducto: (Producto) -> Unit,
    onVolver: () -> Unit
) {
    // 0: Resumen, 1: Usuarios, 2: Tiendas, 3: Catalogo, 4: Pedidos
    var subTab by remember { mutableIntStateOf(0) }
    var filtroTiendas by remember { mutableStateOf<EstadoEmprendimiento?>(null) }
    var dialogoNuevaCat by remember { mutableStateOf(false) }
    var nombreNuevaCat by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel Administrativo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Gobierno de EmprendePiura",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (subTab == 3) {
                FloatingActionButton(onClick = { dialogoNuevaCat = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva categoría")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = subTab,
                edgePadding = 12.dp
            ) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Resumen") })
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Usuarios (${state.stats.totalUsuarios})") }
                )
                Tab(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    text = { Text("Tiendas (${state.stats.emprendimientosPendientes})") }
                )
                Tab(selected = subTab == 3, onClick = { subTab = 3 }, text = { Text("Catálogo") })
                Tab(
                    selected = subTab == 4,
                    onClick = { subTab = 4 },
                    text = { Text("Pedidos (${state.stats.totalOrdenes})") }
                )
            }

            if (state.loading && state.emprendimientos.isEmpty() && state.usuarios.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (subTab) {
                    0 -> AdminStatsTab(
                        state = state,
                        onAbrirUsuarios = { subTab = 1 },
                        onAbrirTiendasAprobadas = {
                            filtroTiendas = EstadoEmprendimiento.APROBADO
                            subTab = 2
                        },
                        onAbrirTiendasPendientes = {
                            filtroTiendas = EstadoEmprendimiento.PENDIENTE
                            subTab = 2
                        },
                        onAbrirCatalogo = { subTab = 3 },
                        onAbrirPedidos = { subTab = 4 }
                    )
                    1 -> AdminUsersTab(
                        usuarios = state.usuarios,
                        miAdminUsuarioId = miAdminUsuarioId,
                        onToggleUsuarioActivo = onToggleUsuarioActivo
                    )
                    2 -> AdminStoresTab(
                        tiendas = state.emprendimientos,
                        filtroInicial = filtroTiendas,
                        onFiltroChange = { filtroTiendas = it },
                        onResolverTienda = onResolverTienda
                    )
                    3 -> AdminCatalogModerationTab(
                        categorias = state.categorias,
                        productos = state.productos,
                        onToggleCategoria = onToggleCategoria,
                        onModerarProducto = onModerarProducto
                    )
                    4 -> AdminOrdersTab(
                        ordenes = state.ordenes,
                        montoTotalVentas = state.stats.montoTotalVentas
                    )
                }
            }
        }
    }

    if (dialogoNuevaCat) {
        AlertDialog(
            onDismissRequest = { dialogoNuevaCat = false },
            title = { Text("Nueva Categoría") },
            text = {
                OutlinedTextField(
                    value = nombreNuevaCat,
                    onValueChange = { nombreNuevaCat = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCrearCategoria(nombreNuevaCat)
                    nombreNuevaCat = ""
                    dialogoNuevaCat = false
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogoNuevaCat = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AdminStatsTab(
    state: AdminUiState,
    onAbrirUsuarios: () -> Unit,
    onAbrirTiendasAprobadas: () -> Unit,
    onAbrirTiendasPendientes: () -> Unit,
    onAbrirCatalogo: () -> Unit,
    onAbrirPedidos: () -> Unit
) {
    val s = state.stats
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Toca cualquier tarjeta para gestionar el detalle en tiempo real.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    titulo = "Usuarios",
                    subtitulo = "Ver DNIs y cuentas",
                    valor = s.totalUsuarios.toString(),
                    color = Color(0xFF2563EB),
                    onClick = onAbrirUsuarios,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    titulo = "Tiendas Activas",
                    subtitulo = "Gestionar aprobadas",
                    valor = s.emprendimientosAprobados.toString(),
                    color = Color(0xFF15803D),
                    onClick = onAbrirTiendasAprobadas,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    titulo = "Pendientes Aprob.",
                    subtitulo = "Revisar solicitudes",
                    valor = s.emprendimientosPendientes.toString(),
                    color = Color(0xFFD97706),
                    onClick = onAbrirTiendasPendientes,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    titulo = "Productos Activos",
                    subtitulo = "Moderar catálogo",
                    valor = s.productosActivos.toString(),
                    color = Color(0xFF0D9488),
                    onClick = onAbrirCatalogo,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    titulo = "Total Pedidos",
                    subtitulo = "Auditar órdenes",
                    valor = s.totalOrdenes.toString(),
                    color = Color(0xFF6366F1),
                    onClick = onAbrirPedidos,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    titulo = "Volumen Ventas",
                    subtitulo = "Detalle económico",
                    valor = PiuraConstants.formatearSoles(s.montoTotalVentas),
                    color = Color(0xFF15803D),
                    onClick = onAbrirPedidos,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    titulo: String,
    subtitulo: String,
    valor: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val press = rememberPressInteraction()
    ElevatedCard(
        onClick = onClick,
        interactionSource = press,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.pressScale(press)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(titulo, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text(subtitulo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AdminStoresTab(
    tiendas: List<Emprendimiento>,
    filtroInicial: EstadoEmprendimiento?,
    onFiltroChange: (EstadoEmprendimiento?) -> Unit,
    onResolverTienda: (Emprendimiento, EstadoEmprendimiento) -> Unit
) {
    var accionPendiente by remember { mutableStateOf<Pair<Emprendimiento, EstadoEmprendimiento>?>(null) }

    val filtradas = remember(tiendas, filtroInicial) {
        if (filtroInicial == null) tiendas else tiendas.filter { it.estado == filtroInicial }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filtroInicial == null,
                onClick = { onFiltroChange(null) },
                label = { Text("Todas (${tiendas.size})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = filtroInicial == EstadoEmprendimiento.PENDIENTE,
                onClick = { onFiltroChange(EstadoEmprendimiento.PENDIENTE) },
                label = {
                    Text(
                        "Pendientes (${tiendas.count { it.estado == EstadoEmprendimiento.PENDIENTE }})",
                        fontSize = 12.sp
                    )
                }
            )
            FilterChip(
                selected = filtroInicial == EstadoEmprendimiento.APROBADO,
                onClick = { onFiltroChange(EstadoEmprendimiento.APROBADO) },
                label = {
                    Text(
                        "Aprobadas (${tiendas.count { it.estado == EstadoEmprendimiento.APROBADO }})",
                        fontSize = 12.sp
                    )
                }
            )
            FilterChip(
                selected = filtroInicial == EstadoEmprendimiento.SUSPENDIDO,
                onClick = { onFiltroChange(EstadoEmprendimiento.SUSPENDIDO) },
                label = {
                    Text(
                        "Rechazadas (${tiendas.count { it.estado == EstadoEmprendimiento.SUSPENDIDO }})",
                        fontSize = 12.sp
                    )
                }
            )
        }

        if (filtradas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.Default.Storefront,
                    titulo = "Sin tiendas en este filtro",
                    subtitulo = "Prueba seleccionando otro estado para ver los emprendimientos registrados."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtradas, key = { it.id }) { t ->
                    val colorEstado = when (t.estado) {
                        EstadoEmprendimiento.PENDIENTE -> Color(0xFFD97706)
                        EstadoEmprendimiento.APROBADO -> Color(0xFF15803D)
                        EstadoEmprendimiento.SUSPENDIDO -> MaterialTheme.colorScheme.error
                    }
                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.nombreTienda, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                StatusPillBadge(t.estado.etiqueta, colorEstado)
                            }
                            Text(
                                text = "${t.categoriaPrincipal} · ${t.zonaPiura} · Tel: ${t.telefonoContacto}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (t.descripcion.isNotBlank()) {
                                Text(t.descripcion, fontSize = 12.5.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (t.estado != EstadoEmprendimiento.APROBADO) {
                                    Button(
                                        onClick = { accionPendiente = t to EstadoEmprendimiento.APROBADO },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Aprobar", fontSize = 12.5.sp)
                                    }
                                }
                                if (t.estado != EstadoEmprendimiento.SUSPENDIDO) {
                                    OutlinedButton(
                                        onClick = { accionPendiente = t to EstadoEmprendimiento.SUSPENDIDO },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Rechazar", fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    accionPendiente?.let { (tienda, nuevoEstado) ->
        val esAprobacion = nuevoEstado == EstadoEmprendimiento.APROBADO
        AlertDialog(
            onDismissRequest = { accionPendiente = null },
            icon = {
                Icon(
                    imageVector = if (esAprobacion) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (esAprobacion) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (esAprobacion) "¿Aprobar emprendimiento?" else "¿Rechazar emprendimiento?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (esAprobacion) {
                        "¿Estás seguro de aprobar \"${tienda.nombreTienda}\"? El titular obtendrá acceso como Emprendedor y podrá publicar productos en el catálogo."
                    } else {
                        "¿Estás seguro de rechazar \"${tienda.nombreTienda}\"? Sus productos dejarán de mostrarse en el catálogo y el titular volverá a rol Comprador."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        accionPendiente = null
                        onResolverTienda(tienda, nuevoEstado)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esAprobacion) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (esAprobacion) "Sí, aprobar" else "Sí, rechazar")
                }
            },
            dismissButton = {
                TextButton(onClick = { accionPendiente = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun AdminCatalogModerationTab(
    categorias: List<Categoria>,
    productos: List<Producto>,
    onToggleCategoria: (Categoria) -> Unit,
    onModerarProducto: (Producto) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("CATEGORÍAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(categorias, key = { "cat_${it.id}" }) { cat ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.nombre, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = cat.activa, onCheckedChange = { onToggleCategoria(cat) })
                }
            }
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text("MODERACIÓN DE PRODUCTOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(productos, key = { "prod_${it.id}" }) { prod ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(prod.titulo, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${prod.nombreTienda} · ${PiuraConstants.formatearSoles(prod.precio)} · ${prod.estado.etiqueta}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onModerarProducto(prod) }) {
                        Icon(
                            imageVector = if (prod.estado == EstadoProducto.ACTIVO) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ocultar o activar",
                            tint = if (prod.estado == EstadoProducto.ACTIVO) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}
