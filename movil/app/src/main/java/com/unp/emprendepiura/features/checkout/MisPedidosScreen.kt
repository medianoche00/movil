package com.unp.emprendepiura.features.checkout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.CodigoEntregaUtil
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.Orden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPedidosScreen(
    ordenes: List<Orden>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onChatearOrden: (Orden) -> Unit,
    onCancelarOrden: (Orden) -> Unit,
    onConfirmarRecepcion: (Orden) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var ordenAConfirmarRecepcion by remember { mutableStateOf<Orden?>(null) }

    fun copiarPin(pin: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Código de Entrega", pin))
        Toast.makeText(context, "PIN de entrega $pin copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Mis Pedidos", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(
                        text = "Seguimiento y confirmación de entrega segura",
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

        when {
            loading && ordenes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            ordenes.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateBox(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        titulo = "Aún no tienes pedidos",
                        subtitulo = "Cuando confirmes una compra en el carrito aparecerá aquí con su estado y código de entrega."
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(ordenes, key = { it.id }) { orden ->
                        val colorEstado = when (orden.estado) {
                            EstadoOrden.PENDIENTE -> Color(0xFFD97706)
                            EstadoOrden.CONFIRMADO -> Color(0xFF2563EB)
                            EstadoOrden.EN_CAMINO -> Color(0xFF0D9488)
                            EstadoOrden.ENTREGADO -> Color(0xFF15803D)
                            EstadoOrden.CANCELADO -> MaterialTheme.colorScheme.error
                        }

                        val pinEntrega = remember(orden.id, orden.codigoOrden) {
                            CodigoEntregaUtil.obtenerCodigoEntrega(orden.id, orden.codigoOrden)
                        }

                        ElevatedCard(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = orden.nombreTienda.ifBlank { "Emprendimiento Piura" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp
                                        )
                                        Text(
                                            text = orden.codigoOrden,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPillBadge(texto = orden.estado.etiqueta, color = colorEstado)
                                }

                                // Tarjeta visual de Código de Entrega (PIN estilo Uber Eats)
                                if (orden.estado == EstadoOrden.CONFIRMADO || orden.estado == EstadoOrden.EN_CAMINO) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { copiarPin(pinEntrega) }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "PIN de confirmación de entrega:",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = CodigoEntregaUtil.formatearPinEspaciado(pinEntrega),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 20.sp,
                                                        letterSpacing = 2.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { copiarPin(pinEntrega) }) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copiar PIN",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "ℹ️ Entrega este PIN al emprendedor únicamente cuando te entregue tu producto en mano.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (orden.estado == EstadoOrden.ENTREGADO) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF15803D).copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Verified, null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Pedido entregado con éxito · Código verificado",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                }

                                // Detalle de productos
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    orden.items.forEach { it ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${it.cantidad}x ${it.tituloSnapshot}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                            Text(PiuraConstants.formatearSoles(it.subtotal), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(orden.tipoEntrega.etiqueta, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(orden.direccionOPunto, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = PiuraConstants.formatearSoles(orden.total),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Botones de accion
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onChatearOrden(orden) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Chatear", fontSize = 12.5.sp)
                                    }

                                    if (orden.estado == EstadoOrden.EN_CAMINO) {
                                        Button(
                                            onClick = { ordenAConfirmarRecepcion = orden },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Recibí mi pedido", fontSize = 12.sp)
                                        }
                                    }

                                    if (orden.estado == EstadoOrden.PENDIENTE) {
                                        OutlinedButton(
                                            onClick = { onCancelarOrden(orden) },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancelar", fontSize = 12.5.sp)
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

    // Modal de confirmacion de recepcion por parte del comprador
    if (ordenAConfirmarRecepcion != null) {
        val o = ordenAConfirmarRecepcion!!
        AlertDialog(
            onDismissRequest = { ordenAConfirmarRecepcion = null },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF15803D), modifier = Modifier.size(36.dp)) },
            title = { Text("¿Confirmar entrega recibida?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Confirmas que recibiste tu pedido de '${o.nombreTienda}' en tus manos y en buen estado? Esta acción marcará la orden como Entregada."
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
                    Text("Sí, ya lo recibí")
                }
            },
            dismissButton = {
                TextButton(onClick = { ordenAConfirmarRecepcion = null }) {
                    Text("Aún no")
                }
            }
        )
    }
}
