package com.unp.emprendepiura.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    state: AdminUiState,
    onRefresh: () -> Unit,
    onResolverTienda: (Emprendimiento, EstadoEmprendimiento) -> Unit,
    onCrearCategoria: (String) -> Unit,
    onToggleCategoria: (Categoria) -> Unit,
    onModerarProducto: (Producto) -> Unit,
    onVolver: () -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
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
            if (subTab == 2) {
                FloatingActionButton(onClick = { dialogoNuevaCat = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva categoría")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = subTab) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Resumen") })
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Tiendas (${state.stats.emprendimientosPendientes})") }
                )
                Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Catálogo") })
            }

            if (state.loading && state.emprendimientos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (subTab) {
                    0 -> AdminStatsTab(state)
                    1 -> AdminStoresTab(state.emprendimientos, onResolverTienda)
                    2 -> AdminCatalogModerationTab(
                        categorias = state.categorias,
                        productos = state.productos,
                        onToggleCategoria = onToggleCategoria,
                        onModerarProducto = onModerarProducto
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
private fun AdminStatsTab(state: AdminUiState) {
    val s = state.stats
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Usuarios", s.totalUsuarios.toString(), Color(0xFF2563EB), Modifier.weight(1f))
                KpiCard("Tiendas Activas", s.emprendimientosAprobados.toString(), Color(0xFF15803D), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Pendientes Aprob.", s.emprendimientosPendientes.toString(), Color(0xFFD97706), Modifier.weight(1f))
                KpiCard("Productos Activos", s.productosActivos.toString(), Color(0xFF0D9488), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Total Pedidos", s.totalOrdenes.toString(), Color(0xFF6366F1), Modifier.weight(1f))
                KpiCard("Volumen Ventas", PiuraConstants.formatearSoles(s.montoTotalVentas), Color(0xFF15803D), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiCard(titulo: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
            Text(titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminStoresTab(
    tiendas: List<Emprendimiento>,
    onResolverTienda: (Emprendimiento, EstadoEmprendimiento) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tiendas, key = { it.id }) { t ->
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
                                onClick = { onResolverTienda(t, EstadoEmprendimiento.APROBADO) },
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
                                onClick = { onResolverTienda(t, EstadoEmprendimiento.SUSPENDIDO) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Suspender", fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }
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
                            text = "${PiuraConstants.formatearSoles(prod.precio)} · ${prod.estado.etiqueta}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
