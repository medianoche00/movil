package com.unp.emprendepiura.features.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.StatusPillBadge
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Mis Pedidos", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(
                        text = "Seguimiento de compras y entregas",
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
                        subtitulo = "Cuando confirmes una compra en el carrito aparecerá aquí con su estado."
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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

                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${orden.codigoOrden} · ${orden.nombreTienda}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusPillBadge(texto = orden.estado.etiqueta, color = colorEstado)
                                }

                                orden.items.forEach { it ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${it.cantidad}x ${it.tituloSnapshot}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text(PiuraConstants.formatearSoles(it.subtotal), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${orden.metodoPago.etiqueta}\n${orden.direccionOPunto}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = PiuraConstants.formatearSoles(orden.total),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

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
}
