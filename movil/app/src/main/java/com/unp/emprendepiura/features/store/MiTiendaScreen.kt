package com.unp.emprendepiura.features.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.ProductThumbnail
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Producto

/*
 * Archivo: MiTiendaScreen.kt
 * Proposito: Gestion del inventario y publicacion de productos del emprendimiento,
 *            con acceso rapido a la pestana Pedidos -> Por Entregar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTiendaScreen(
    storeState: StoreUiState,
    entregasPendientesCount: Int,
    categorias: List<Categoria>,
    onVolver: () -> Unit,
    onIrAPedidosPorEntregar: () -> Unit,
    onGuardarProducto: (
        productoExistente: Producto?,
        titulo: String,
        descripcion: String,
        precioTexto: String,
        stockTexto: String,
        categoriaId: String,
        zonaPiura: String,
        imagenUri: android.net.Uri?,
        onDone: () -> Unit
    ) -> Unit,
    onToggleEstadoProducto: (Producto) -> Unit
) {
    val tienda = storeState.miTienda ?: return
    var editorAbierto by remember { mutableStateOf(false) }
    var productoEnEdicion by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tienda.nombreTienda, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${tienda.zonaPiura} · Catálogo de productos",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            val fabPress = rememberPressInteraction()
            FloatingActionButton(
                onClick = {
                    productoEnEdicion = null
                    editorAbierto = true
                },
                interactionSource = fabPress,
                modifier = Modifier.pressScale(fabPress)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Acceso directo a entregas pendientes en la pestana Pedidos
            val shortcutPress = rememberPressInteraction()
            Surface(
                onClick = onIrAPedidosPorEntregar,
                interactionSource = shortcutPress,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .pressScale(shortcutPress)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (entregasPendientesCount > 0) {
                                "Tienes $entregasPendientesCount pedidos por entregar"
                            } else {
                                "Ver pedidos recibidos de tu tienda"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gestiona entregas y valida PIN dinámico en Pedidos",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (storeState.misProductos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateBox(
                        icon = Icons.Default.Inventory2,
                        titulo = "Aún no publicas productos",
                        subtitulo = "Usa el botón + para agregar tu primer artículo al catálogo.",
                        accionTexto = "Publicar producto",
                        onAccionClick = {
                            productoEnEdicion = null
                            editorAbierto = true
                        }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(storeState.misProductos, key = { it.id }) { prod ->
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(placementSpec = applePushSpring())
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductThumbnail(
                                    imageUrl = prod.imagenUrl,
                                    categoria = prod.nombreCategoria,
                                    modifier = Modifier.size(64.dp)
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(prod.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${PiuraConstants.formatearSoles(prod.precio)} · Stock ${prod.stock}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.primary)
                                    StatusPillBadge(
                                        texto = prod.estado.name,
                                        color = if (prod.estado == EstadoProducto.ACTIVO) Color(0xFF15803D) else Color(0xFFD97706)
                                    )
                                }
                                IconButton(onClick = {
                                    productoEnEdicion = prod
                                    editorAbierto = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { onToggleEstadoProducto(prod) }) {
                                    Icon(
                                        imageVector = if (prod.estado == EstadoProducto.ACTIVO) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Pausar o activar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorAbierto) {
        ProductoEditorDialog(
            productoExistente = productoEnEdicion,
            categorias = categorias,
            zonaDefecto = tienda.zonaPiura,
            saving = storeState.saving,
            onDismiss = { editorAbierto = false },
            onGuardar = { titulo, desc, precio, stock, catId, zona, uri ->
                onGuardarProducto(productoEnEdicion, titulo, desc, precio, stock, catId, zona, uri) {
                    editorAbierto = false
                }
            }
        )
    }
}
