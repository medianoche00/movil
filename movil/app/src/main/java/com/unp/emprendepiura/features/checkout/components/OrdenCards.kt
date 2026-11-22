package com.unp.emprendepiura.features.checkout.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.Orden

/*
 * Archivo: OrdenCards.kt
 * Proposito: Tarjetas modulares para pedidos como Comprador (Mis Compras) y como Emprendedor
 *            (Por Entregar), ocultando el chat cuando el pedido ya se encuentra Entregado.
 */
@Composable
fun OrdenCompraCard(
    orden: Orden,
    onCopiarPin: (String) -> Unit,
    onChatear: () -> Unit,
    onSolicitarConfirmarRecepcion: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorEstado = colorParaEstado(orden.estado)
    val pedidoFinalizado = orden.estado == EstadoOrden.ENTREGADO || orden.estado == EstadoOrden.CANCELADO

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
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
                        fontSize = 15.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = orden.codigoOrden,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPillBadge(texto = orden.estado.etiqueta, color = colorEstado)
            }

            if (orden.estado == EstadoOrden.CONFIRMADO || orden.estado == EstadoOrden.EN_CAMINO) {
                DynamicDeliveryPinCard(
                    ordenId = orden.id,
                    codigoOrden = orden.codigoOrden,
                    onCopiarPin = onCopiarPin
                )
            } else if (orden.estado == EstadoOrden.ENTREGADO) {
                BannerEntregaVerificada(texto = "Pedido entregado con éxito · Código verificado")
            }

            OrdenItemsResumen(orden = orden)

            if (!pedidoFinalizado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chatPress = rememberPressInteraction()
                    OutlinedButton(
                        onClick = onChatear,
                        interactionSource = chatPress,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .pressScale(chatPress)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chatear", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (orden.estado == EstadoOrden.EN_CAMINO) {
                        val confirmPress = rememberPressInteraction()
                        Button(
                            onClick = onSolicitarConfirmarRecepcion,
                            interactionSource = confirmPress,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .pressScale(confirmPress)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Confirmar Pedido", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (orden.estado == EstadoOrden.PENDIENTE) {
                        val cancelPress = rememberPressInteraction()
                        OutlinedButton(
                            onClick = onCancelar,
                            interactionSource = cancelPress,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                            modifier = Modifier
                                .weight(1f)
                                .pressScale(cancelPress)
                        ) {
                            Text("Cancelar", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdenVentaCard(
    orden: Orden,
    onVerComprobante: (String) -> Unit,
    onChatearComprador: () -> Unit,
    onAvanzarEstado: (EstadoOrden) -> Unit,
    onAbrirValidacionPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorEstado = colorParaEstado(orden.estado)
    val pedidoFinalizado = orden.estado == EstadoOrden.ENTREGADO || orden.estado == EstadoOrden.CANCELADO

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
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
                        text = orden.nombreComprador.ifBlank { "Comprador EmprendePiura" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${orden.codigoOrden} · ${orden.metodoPago.etiqueta}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPillBadge(texto = orden.estado.etiqueta, color = colorEstado)
            }

            if (orden.comprobanteUrl.isNotBlank()) {
                val compPress = rememberPressInteraction()
                OutlinedButton(
                    onClick = { onVerComprobante(orden.comprobanteUrl) },
                    interactionSource = compPress,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.pressScale(compPress)
                ) {
                    Icon(Icons.Default.Receipt, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            append("Ver comprobante Yape/Plin")
                            if (orden.codigoOperacion.isNotBlank()) append(" (${orden.codigoOperacion})")
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OrdenItemsResumen(orden = orden)

            if (orden.estado == EstadoOrden.ENTREGADO) {
                BannerEntregaVerificada(texto = "Entrega completada · Código verificado")
            } else if (!pedidoFinalizado) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chatPress = rememberPressInteraction()
                    OutlinedButton(
                        onClick = onChatearComprador,
                        interactionSource = chatPress,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .pressScale(chatPress)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chatear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val actionPress = rememberPressInteraction()
                    when (orden.estado) {
                        EstadoOrden.PENDIENTE -> {
                            Button(
                                onClick = { onAvanzarEstado(EstadoOrden.CONFIRMADO) },
                                interactionSource = actionPress,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .pressScale(actionPress)
                            ) {
                                Text("Confirmar Pedido", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EstadoOrden.CONFIRMADO -> {
                            Button(
                                onClick = { onAvanzarEstado(EstadoOrden.EN_CAMINO) },
                                interactionSource = actionPress,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .pressScale(actionPress)
                            ) {
                                Text("Marcar En Camino", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EstadoOrden.EN_CAMINO -> {
                            Button(
                                onClick = onAbrirValidacionPin,
                                interactionSource = actionPress,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                modifier = Modifier
                                    .weight(1.45f)
                                    .pressScale(actionPress)
                            ) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Validar PIN (6 dígitos)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdenItemsResumen(orden: Orden) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        orden.items.forEach { it ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${it.cantidad}x ${it.tituloSnapshot}",
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = PiuraConstants.formatearSoles(it.subtotal),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
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
            Text(
                text = orden.direccionOPunto,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (orden.notasComprador.isNotBlank()) {
                Text(
                    text = "Nota: ${orden.notasComprador}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = PiuraConstants.formatearSoles(orden.total),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BannerEntregaVerificada(texto: String) {
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
                text = texto,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF15803D)
            )
        }
    }
}

@Composable
private fun colorParaEstado(estado: EstadoOrden): Color = when (estado) {
    EstadoOrden.PENDIENTE -> Color(0xFFD97706)
    EstadoOrden.CONFIRMADO -> Color(0xFF2563EB)
    EstadoOrden.EN_CAMINO -> Color(0xFF0D9488)
    EstadoOrden.ENTREGADO -> Color(0xFF15803D)
    EstadoOrden.CANCELADO -> MaterialTheme.colorScheme.error
}
