package com.unp.emprendepiura.features.checkout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.ProductThumbnail
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.CartItemLocal
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.MetodoPago
import com.unp.emprendepiura.domain.model.TipoEntrega

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartAgrupado: Map<String, List<CartItemLocal>>,
    tiendas: Map<String, Emprendimiento>,
    procesandoCheckout: Boolean,
    onChangeQty: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onConfirmarCheckout: (
        emprendimientoId: String,
        tipoEntrega: TipoEntrega,
        direccionOPunto: String,
        metodoPago: MetodoPago,
        comprobanteUri: Uri?,
        codigoOperacion: String,
        notas: String,
        items: List<CartItemLocal>
    ) -> Unit,
    onIrAExplorar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tiendaParaCheckoutId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Carrito de Compras", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(
                        text = "Pedidos agrupados por emprendimiento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        if (cartAgrupado.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.Default.ShoppingCart,
                    titulo = "Tu carrito está vacío",
                    subtitulo = "Explora productos de emprendedores piuranos y agrégalos aquí.",
                    accionTexto = "Explorar catálogo",
                    onAccionClick = onIrAExplorar
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(cartAgrupado.entries.toList(), key = { it.key }) { (tiendaId, itemsTienda) ->
                    val tienda = tiendas[tiendaId]
                    val totalTienda = itemsTienda.sumOf { it.subtotal }

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = tienda?.nombreTienda ?: itemsTienda.first().producto.nombreTienda,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = PiuraConstants.formatearSoles(totalTienda),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                            itemsTienda.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ProductThumbnail(
                                        imageUrl = item.producto.imagenUrl,
                                        categoria = item.producto.nombreCategoria,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.producto.titulo,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${PiuraConstants.formatearSoles(item.producto.precio)} c/u",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onChangeQty(item.producto.id, -1) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Quitar uno", modifier = Modifier.size(16.dp))
                                        }
                                        Text("${item.cantidad}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        IconButton(
                                            onClick = { onChangeQty(item.producto.id, 1) },
                                            enabled = item.cantidad < item.producto.stock,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Agregar uno", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onRemoveItem(item.producto.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { tiendaParaCheckoutId = tiendaId },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirmar pedido a esta tienda", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    val checkoutTiendaId = tiendaParaCheckoutId
    val checkoutItems = if (checkoutTiendaId != null) cartAgrupado[checkoutTiendaId].orEmpty() else emptyList()
    if (checkoutTiendaId != null && checkoutItems.isNotEmpty()) {
        CheckoutModalDialog(
            tienda = tiendas[checkoutTiendaId],
            items = checkoutItems,
            procesando = procesandoCheckout,
            onDismiss = { if (!procesandoCheckout) tiendaParaCheckoutId = null },
            onConfirmar = { entrega, punto, pago, comprobante, op, notas ->
                onConfirmarCheckout(checkoutTiendaId, entrega, punto, pago, comprobante, op, notas, checkoutItems)
                tiendaParaCheckoutId = null
            }
        )
    }
}

@Composable
private fun CheckoutModalDialog(
    tienda: Emprendimiento?,
    items: List<CartItemLocal>,
    procesando: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (TipoEntrega, String, MetodoPago, Uri?, String, String) -> Unit
) {
    val context = LocalContext.current
    var tipoEntrega by remember { mutableStateOf(TipoEntrega.PUNTO_ENCUENTRO) }
    var direccionOPunto by remember {
        mutableStateOf(tienda?.puntoEncuentroHabitual?.ifBlank { PiuraConstants.PUNTOS_ENCUENTRO_UNP.first() } ?: PiuraConstants.PUNTOS_ENCUENTRO_UNP.first())
    }
    var metodoPago by remember { mutableStateOf(MetodoPago.CONTRA_ENTREGA) }
    var comprobanteUri by remember { mutableStateOf<Uri?>(null) }
    var codigoOperacion by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) comprobanteUri = uri
    }

    val total = items.sumOf { it.subtotal }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Finalizar Pedido · ${tienda?.nombreTienda ?: "Tienda"}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Total a pagar: ${PiuraConstants.formatearSoles(total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text("1. Modalidad de entrega", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipoEntrega == TipoEntrega.PUNTO_ENCUENTRO,
                        onClick = { tipoEntrega = TipoEntrega.PUNTO_ENCUENTRO },
                        label = { Text("Punto encuentro", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = tipoEntrega == TipoEntrega.DELIVERY,
                        onClick = {
                            tipoEntrega = TipoEntrega.DELIVERY
                            direccionOPunto = ""
                        },
                        label = { Text("Delivery Piura", fontSize = 12.sp) }
                    )
                }

                if (tipoEntrega == TipoEntrega.PUNTO_ENCUENTRO) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PiuraConstants.PUNTOS_ENCUENTRO_UNP.forEach { punto ->
                            AssistChip(
                                onClick = { direccionOPunto = punto },
                                label = { Text(punto, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = direccionOPunto,
                    onValueChange = { direccionOPunto = it },
                    label = {
                        Text(
                            if (tipoEntrega == TipoEntrega.PUNTO_ENCUENTRO) "Punto de encuentro *" else "Dirección y referencia *"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("2. Método de pago", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetodoPago.entries.forEach { mp ->
                        Surface(
                            onClick = { metodoPago = mp },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (metodoPago == mp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            color = if (metodoPago == mp) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = metodoPago == mp, onClick = { metodoPago = mp })
                                Spacer(Modifier.width(6.dp))
                                Text(mp.etiqueta, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (metodoPago == MetodoPago.YAPE_PLIN) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Yape / Plin destino: ${tienda?.numeroYapePlin?.ifBlank { tienda.telefonoContacto } ?: "Consultar por chat"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF6D28D9)
                            )
                            OutlinedButton(
                                onClick = { picker.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (comprobanteUri == null) "Adjuntar captura Yape/Plin *" else "Cambiar captura adjunta")
                            }
                            if (comprobanteUri != null) {
                                AsyncImage(
                                    model = comprobanteUri,
                                    contentDescription = "Comprobante",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            OutlinedTextField(
                                value = codigoOperacion,
                                onValueChange = { codigoOperacion = it },
                                label = { Text("Código de operación (opcional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Nota o horario sugerido (opcional)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !procesando,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val check = com.unp.emprendepiura.domain.validator.MarketplaceValidator.validarCheckout(
                                direccionOPunto = direccionOPunto,
                                metodoPago = metodoPago,
                                tieneComprobante = comprobanteUri != null
                            )
                            if (check != null) {
                                android.widget.Toast.makeText(context, check, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onConfirmar(tipoEntrega, direccionOPunto, metodoPago, comprobanteUri, codigoOperacion, notas)
                            }
                        },
                        enabled = !procesando,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Enviar Pedido", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
