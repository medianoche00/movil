package com.unp.emprendepiura.features.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.ProductThumbnail
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.CodigoEntregaUtil
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.EstadoProducto
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTiendaScreen(
    storeState: StoreUiState,
    ordenesRecibidas: List<Orden>,
    categorias: List<Categoria>,
    onVolver: () -> Unit,
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
    onToggleEstadoProducto: (Producto) -> Unit,
    onAvanzarOrden: (Orden, EstadoOrden) -> Unit,
    onConfirmarEntregaConPin: ((Orden, String) -> Unit)? = null
) {
    val tienda = storeState.miTienda ?: return
    var subTab by remember { mutableIntStateOf(0) }
    var editorAbierto by remember { mutableStateOf(false) }
    var productoEnEdicion by remember { mutableStateOf<Producto?>(null) }
    var comprobanteVerUrl by remember { mutableStateOf<String?>(null) }
    var ordenParaPinEntrega by remember { mutableStateOf<Orden?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tienda.nombreTienda, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${tienda.zonaPiura} · ${tienda.estado.etiqueta}",
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
            if (subTab == 0) {
                FloatingActionButton(
                    onClick = {
                        productoEnEdicion = null
                        editorAbierto = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo producto")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = subTab) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text("Productos (${storeState.misProductos.size})") }
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Pedidos (${ordenesRecibidas.size})") }
                )
            }

            if (subTab == 0) {
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(storeState.misProductos, key = { it.id }) { prod ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProductThumbnail(prod.imagenUrl, prod.nombreCategoria, Modifier.size(56.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = "${PiuraConstants.formatearSoles(prod.precio)} · Stock ${prod.stock}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        StatusPillBadge(
                                            texto = prod.estado.etiqueta,
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
            } else {
                if (ordenesRecibidas.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateBox(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            titulo = "Sin pedidos recibidos",
                            subtitulo = "Cuando un comprador confirme una orden de tu tienda la verás aquí."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ordenesRecibidas, key = { it.id }) { orden ->
                            ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${orden.codigoOrden} · ${PiuraConstants.formatearSoles(orden.total)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        StatusPillBadge(orden.estado.etiqueta, Color(0xFF2563EB))
                                    }
                                    orden.items.forEach { it ->
                                        Text("• ${it.cantidad}x ${it.tituloSnapshot}", fontSize = 13.sp)
                                    }
                                    Text(
                                        text = "${orden.tipoEntrega.etiqueta}: ${orden.direccionOPunto}\nPago: ${orden.metodoPago.etiqueta}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (orden.comprobanteUrl.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { comprobanteVerUrl = orden.comprobanteUrl },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Ver captura Yape / Plin")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        when (orden.estado) {
                                            EstadoOrden.PENDIENTE -> {
                                                Button(onClick = { onAvanzarOrden(orden, EstadoOrden.CONFIRMADO) }, modifier = Modifier.weight(1f)) {
                                                    Text("Confirmar", fontSize = 12.5.sp)
                                                }
                                                OutlinedButton(onClick = { onAvanzarOrden(orden, EstadoOrden.CANCELADO) }, modifier = Modifier.weight(1f)) {
                                                    Text("Cancelar", fontSize = 12.5.sp)
                                                }
                                            }
                                            EstadoOrden.CONFIRMADO -> {
                                                Button(onClick = { onAvanzarOrden(orden, EstadoOrden.EN_CAMINO) }, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Marcar Listo / En camino", fontSize = 12.5.sp)
                                                }
                                            }
                                            EstadoOrden.EN_CAMINO -> {
                                                Button(
                                                    onClick = { ordenParaPinEntrega = orden },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Validar Entrega con PIN", fontSize = 12.5.sp)
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
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
            onDismiss = { if (!storeState.saving) editorAbierto = false },
            onGuardar = { t, d, p, s, catId, z, uri ->
                onGuardarProducto(productoEnEdicion, t, d, p, s, catId, z, uri) {
                    editorAbierto = false
                }
            }
        )
    }

    if (comprobanteVerUrl != null) {
        Dialog(onDismissRequest = { comprobanteVerUrl = null }) {
            Surface(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Comprobante adjunto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    AsyncImage(
                        model = comprobanteVerUrl,
                        contentDescription = "Comprobante de pago",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    TextButton(onClick = { comprobanteVerUrl = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Modal de confirmación de entrega con código PIN (antifraude estilo Uber Eats)
    if (ordenParaPinEntrega != null) {
        val orden = ordenParaPinEntrega!!
        var pinIngresado by remember { mutableStateOf("") }
        var errorPin by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { ordenParaPinEntrega = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "Código de Entrega Segura",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Para validar la entrega de ${orden.codigoOrden} y evitar fraudes, solicita al comprador el código PIN de 4 dígitos que figura en su aplicación:",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pinIngresado,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinIngresado = it
                                errorPin = null
                            }
                        },
                        placeholder = { Text("Ej: 4829") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        isError = errorPin != null,
                        supportingText = errorPin?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (CodigoEntregaUtil.validarCodigoEntrega(orden.id, orden.codigoOrden, pinIngresado)) {
                            val pin = pinIngresado
                            ordenParaPinEntrega = null
                            if (onConfirmarEntregaConPin != null) {
                                onConfirmarEntregaConPin(orden, pin)
                            } else {
                                onAvanzarOrden(orden, EstadoOrden.ENTREGADO)
                            }
                        } else {
                            errorPin = "Código incorrecto. Solicita al comprador el PIN en 'Mis Pedidos'."
                        }
                    },
                    enabled = pinIngresado.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Confirmar Entrega")
                }
            },
            dismissButton = {
                TextButton(onClick = { ordenParaPinEntrega = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
