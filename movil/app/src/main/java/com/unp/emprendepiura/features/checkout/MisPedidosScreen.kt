package com.unp.emprendepiura.features.checkout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.util.CodigoEntregaUtil
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.features.checkout.components.OrdenCompraCard
import com.unp.emprendepiura.features.checkout.components.OrdenVentaCard
import com.unp.emprendepiura.features.checkout.components.ValidarPinEntregaDialog

enum class PedidosSubTab {
    POR_ENTREGAR,
    MIS_COMPRAS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPedidosScreen(
    ordenesCompra: List<Orden>,
    ordenesVenta: List<Orden>,
    tieneTiendaAprobada: Boolean,
    subTabActual: PedidosSubTab,
    onSelectSubTab: (PedidosSubTab) -> Unit,
    loading: Boolean,
    onRefresh: () -> Unit,
    onChatearCompra: (Orden) -> Unit,
    onCancelarOrden: (Orden) -> Unit,
    onConfirmarRecepcion: (Orden) -> Unit,
    onChatearVenta: (Orden) -> Unit,
    onAvanzarOrdenVenta: (Orden, EstadoOrden) -> Unit,
    onConfirmarEntregaConPin: (Orden, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var ordenAConfirmarRecepcion by remember { mutableStateOf<Orden?>(null) }
    var ordenParaValidarPin by remember { mutableStateOf<Orden?>(null) }
    var comprobanteVerUrl by remember { mutableStateOf<String?>(null) }

    val tabEfectiva = if (tieneTiendaAprobada) subTabActual else PedidosSubTab.MIS_COMPRAS
    val entregasActivas = remember(ordenesVenta) {
        ordenesVenta.count { it.estado != EstadoOrden.ENTREGADO && it.estado != EstadoOrden.CANCELADO }
    }
    val comprasActivas = remember(ordenesCompra) {
        ordenesCompra.count { it.estado != EstadoOrden.ENTREGADO && it.estado != EstadoOrden.CANCELADO }
    }

    fun copiarPin(pin: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val formateado = CodigoEntregaUtil.formatearPin6(pin)
        clipboard.setPrimaryClip(ClipData.newPlainText("PIN de Entrega", pin))
        Toast.makeText(context, "PIN dinámico $formateado copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (tieneTiendaAprobada) "Pedidos" else "Mis Pedidos",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = if (tieneTiendaAprobada) {
                            "Entregas de tu tienda y tus compras personales"
                        } else {
                            "Seguimiento y PIN dinámico de entrega segura"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar pedidos")
                }
            }
        )

        if (tieneTiendaAprobada) {
            PedidosSegmentedSwitch(
                tabActiva = tabEfectiva,
                entregasActivas = entregasActivas,
                comprasActivas = comprasActivas,
                onSelect = onSelectSubTab,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        AnimatedContent(
            targetState = tabEfectiva,
            transitionSpec = {
                fadeIn(tween(220, easing = AppleNavigationEasing)) togetherWith fadeOut(tween(150))
            },
            modifier = Modifier.weight(1f),
            label = "pedidosDualMode"
        ) { tab ->
            val lista = if (tab == PedidosSubTab.POR_ENTREGAR) ordenesVenta else ordenesCompra

            when {
                loading && lista.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                lista.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateBox(
                            icon = if (tab == PedidosSubTab.POR_ENTREGAR) Icons.Default.LocalShipping else Icons.AutoMirrored.Filled.ReceiptLong,
                            titulo = if (tab == PedidosSubTab.POR_ENTREGAR) "Sin pedidos por entregar" else "Aún no tienes compras",
                            subtitulo = if (tab == PedidosSubTab.POR_ENTREGAR) {
                                "Cuando un cliente confirme un pedido en tu emprendimiento aparecerá aquí."
                            } else {
                                "Cuando confirmes una compra en el carrito aparecerá aquí con su código dinámico."
                            }
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (tab == PedidosSubTab.POR_ENTREGAR) {
                            items(ordenesVenta, key = { it.id }) { orden ->
                                OrdenVentaCard(
                                    orden = orden,
                                    onVerComprobante = { comprobanteVerUrl = it },
                                    onChatearComprador = { onChatearVenta(orden) },
                                    onAvanzarEstado = { nuevo -> onAvanzarOrdenVenta(orden, nuevo) },
                                    onAbrirValidacionPin = { ordenParaValidarPin = orden },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(240),
                                        placementSpec = applePushSpring()
                                    )
                                )
                            }
                        } else {
                            items(ordenesCompra, key = { it.id }) { orden ->
                                OrdenCompraCard(
                                    orden = orden,
                                    onCopiarPin = ::copiarPin,
                                    onChatear = { onChatearCompra(orden) },
                                    onSolicitarConfirmarRecepcion = { ordenAConfirmarRecepcion = orden },
                                    onCancelar = { onCancelarOrden(orden) },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(240),
                                        placementSpec = applePushSpring()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (ordenAConfirmarRecepcion != null) {
        val o = ordenAConfirmarRecepcion!!
        AlertDialog(
            onDismissRequest = { ordenAConfirmarRecepcion = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF15803D), modifier = Modifier.size(34.dp)) },
            title = { Text("¿Confirmar entrega recibida?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Confirmas que recibiste tu pedido de '${o.nombreTienda}' en tus manos y en buen estado? El pedido pasará automáticamente a Entregado."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        ordenAConfirmarRecepcion = null
                        onConfirmarRecepcion(o)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Sí, ya lo recibí", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ordenAConfirmarRecepcion = null }) {
                    Text("Aún no")
                }
            }
        )
    }

    if (ordenParaValidarPin != null) {
        val orden = ordenParaValidarPin!!
        ValidarPinEntregaDialog(
            orden = orden,
            onConfirmarPin = { pin ->
                ordenParaValidarPin = null
                onConfirmarEntregaConPin(orden, pin)
            },
            onDismiss = { ordenParaValidarPin = null }
        )
    }

    if (comprobanteVerUrl != null) {
        Dialog(onDismissRequest = { comprobanteVerUrl = null }) {
            Surface(shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Comprobante de pago", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    AsyncImage(
                        model = comprobanteVerUrl,
                        contentDescription = "Comprobante Yape/Plin",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Button(
                        onClick = { comprobanteVerUrl = null },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PedidosSegmentedSwitch(
    tabActiva: PedidosSubTab,
    entregasActivas: Int,
    comprasActivas: Int,
    onSelect: (PedidosSubTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentedTabButton(
                selected = tabActiva == PedidosSubTab.POR_ENTREGAR,
                icon = Icons.Default.LocalShipping,
                titulo = "Por Entregar",
                badgeCount = entregasActivas,
                onClick = { onSelect(PedidosSubTab.POR_ENTREGAR) },
                modifier = Modifier.weight(1f)
            )
            SegmentedTabButton(
                selected = tabActiva == PedidosSubTab.MIS_COMPRAS,
                icon = Icons.Default.LocalMall,
                titulo = "Mis Compras",
                badgeCount = comprasActivas,
                onClick = { onSelect(PedidosSubTab.MIS_COMPRAS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentedTabButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val press = rememberPressInteraction()
    Surface(
        onClick = onClick,
        interactionSource = press,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.pressScale(press)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = titulo,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (badgeCount > 0) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
