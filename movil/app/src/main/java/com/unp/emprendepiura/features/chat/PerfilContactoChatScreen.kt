package com.unp.emprendepiura.features.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.ProductThumbnail
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Producto
import com.unp.emprendepiura.domain.model.Usuario

/*
 * Archivo: PerfilContactoChatScreen.kt
 * Proposito: Pantalla completa estilo Apple para consultar el perfil del contacto del chat
 *            (Emprendimiento con catalogo/Yape/contacto o Comprador sin exponer su DNI).
 */
sealed interface PerfilChatObjetivo {
    data class EmprendimientoPerfil(
        val tienda: Emprendimiento?,
        val nombreFallback: String,
        val productosTienda: List<Producto>
    ) : PerfilChatObjetivo

    data class CompradorPerfil(
        val comprador: Usuario?,
        val nombreFallback: String,
        val entregasConTienda: Int
    ) : PerfilChatObjetivo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilContactoChatScreen(
    objetivo: PerfilChatObjetivo,
    conversacion: Conversacion?,
    miUsuarioId: String,
    darkTheme: Boolean,
    contraparteEnLinea: Boolean,
    onVolver: () -> Unit,
    onIrAlChat: () -> Unit,
    onAbrirProducto: (Producto) -> Unit,
    onBloquear: () -> Unit,
    onDesbloquear: () -> Unit,
    onEliminarConversacion: () -> Unit
) {
    val palette = rememberEmprendeChatPalette(darkTheme)
    val context = LocalContext.current

    var mostrarConfirmacionBloqueo by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }

    val estaBloqueada = conversacion?.estaBloqueada == true
    val bloqueadaPorMi = conversacion?.bloqueadaPorMi(miUsuarioId) == true
    val contactoNoDisponible = estaBloqueada && !bloqueadaPorMi

    val tituloNombre = when (objetivo) {
        is PerfilChatObjetivo.EmprendimientoPerfil -> objetivo.tienda?.nombreTienda ?: objetivo.nombreFallback
        is PerfilChatObjetivo.CompradorPerfil -> objetivo.comprador?.nombre ?: objetivo.nombreFallback
    }.ifBlank { "Usuario EmprendePiura" }

    val iniciales = when (objetivo) {
        is PerfilChatObjetivo.EmprendimientoPerfil -> tituloNombre.take(1).uppercase()
        is PerfilChatObjetivo.CompradorPerfil -> objetivo.comprador?.iniciales?.ifBlank { tituloNombre.take(1).uppercase() }
            ?: tituloNombre.take(1).uppercase()
    }

    val telefono = when (objetivo) {
        is PerfilChatObjetivo.EmprendimientoPerfil -> objetivo.tienda?.telefonoContacto.orEmpty()
        is PerfilChatObjetivo.CompradorPerfil -> objetivo.comprador?.telefono.orEmpty()
    }

    fun copiar(label: String, valor: String) {
        if (valor.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, valor))
        Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = palette.canvasBackground,
        topBar = {
            Surface(color = palette.barSurface) {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.barSurface),
                        navigationIcon = {
                            IconButton(onClick = onVolver) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        },
                        title = {
                            Text(
                                text = when (objetivo) {
                                    is PerfilChatObjetivo.EmprendimientoPerfil -> "Perfil del Emprendimiento"
                                    is PerfilChatObjetivo.CompradorPerfil -> "Perfil del Comprador"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        },
                        actions = {
                            StatusPillBadge(
                                texto = if (objetivo is PerfilChatObjetivo.EmprendimientoPerfil) "Tienda" else "Comprador",
                                color = palette.accentPrimary,
                                modifier = Modifier.padding(end = 14.dp)
                            )
                        }
                    )
                    HorizontalDivider(color = palette.barDivider)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera hero degradada estilo Apple
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.accentPrimary.copy(alpha = if (darkTheme) 0.18f else 0.10f),
                                palette.barSurface
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(86.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                            .background(palette.avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iniciales,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = palette.avatarText
                        )
                    }
                    if (contraparteEnLinea && !estaBloqueada) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(palette.barSurface)
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(palette.onlineDot)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tituloNombre,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verificado",
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val etiquetaRol = when (objetivo) {
                        is PerfilChatObjetivo.EmprendimientoPerfil -> objetivo.tienda?.categoriaPrincipal?.ifBlank { "Emprendimiento" } ?: "Emprendimiento"
                        is PerfilChatObjetivo.CompradorPerfil -> "Comprador verificado"
                    }
                    val zona = when (objetivo) {
                        is PerfilChatObjetivo.EmprendimientoPerfil -> objetivo.tienda?.zonaPiura?.ifBlank { "Piura / UNP" } ?: "Piura / UNP"
                        is PerfilChatObjetivo.CompradorPerfil -> objetivo.comprador?.zonaPiura?.ifBlank { "Piura / UNP" } ?: "Piura / UNP"
                    }
                    StatusPillBadge(texto = etiquetaRol, color = palette.accentPrimary)
                    StatusPillBadge(texto = zona, color = Color(0xFF0D9488))
                }

                Spacer(Modifier.height(4.dp))

                // Fila de acciones rapidas estilo Contacto iOS (oculta si el contacto no esta disponible)
                if (!contactoNoDisponible) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickContactActionTile(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            tint = palette.accentPrimary,
                            label = "Mensaje",
                            palette = palette,
                            onClick = onIrAlChat,
                            modifier = Modifier.weight(1f)
                        )
                        QuickContactActionTile(
                            icon = Icons.Default.Phone,
                            tint = palette.onlineDot,
                            label = "Teléfono",
                            palette = palette,
                            enabled = telefono.isNotBlank() && !estaBloqueada,
                            onClick = { copiar("Teléfono", telefono) },
                            modifier = Modifier.weight(1f)
                        )
                        when (objetivo) {
                            is PerfilChatObjetivo.EmprendimientoPerfil -> {
                                val yape = objetivo.tienda?.numeroYapePlin.orEmpty()
                                QuickContactActionTile(
                                    icon = Icons.Default.Payments,
                                    tint = palette.accentWarmGold,
                                    label = "Yape/Plin",
                                    palette = palette,
                                    enabled = yape.isNotBlank() && !estaBloqueada,
                                    onClick = { copiar("Yape/Plin", yape) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            is PerfilChatObjetivo.CompradorPerfil -> {
                                QuickContactActionTile(
                                    icon = Icons.Default.CheckCircle,
                                    tint = Color(0xFF15803D),
                                    label = "${objetivo.entregasConTienda} entregas",
                                    palette = palette,
                                    enabled = false,
                                    onClick = {},
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    StatusPillBadge(texto = "No disponible en este momento", color = palette.incomingMeta)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (objetivo) {
                    is PerfilChatObjetivo.EmprendimientoPerfil -> {
                        val tienda = objetivo.tienda
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = palette.barSurface,
                            border = BorderStroke(1.dp, palette.barDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "ACERCA DEL EMPRENDIMIENTO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = tienda?.descripcion?.ifBlank { "Emprendimiento local verificado de EmprendePiura." }
                                        ?: "Emprendimiento local verificado de EmprendePiura.",
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                )
                                HorizontalDivider(color = palette.barDivider)
                                InfoDetailRow(
                                    icon = Icons.Default.Place,
                                    tint = palette.accentPrimary,
                                    etiqueta = "Punto de entrega habitual",
                                    valor = tienda?.puntoEncuentroHabitual?.ifBlank { "Campus Universitario UNP" }
                                        ?: "Campus Universitario UNP"
                                )
                                if (!estaBloqueada && telefono.isNotBlank()) {
                                    InfoDetailRow(
                                        icon = Icons.Default.Phone,
                                        tint = palette.onlineDot,
                                        etiqueta = "Teléfono de contacto",
                                        valor = telefono,
                                        onCopy = { copiar("Teléfono", telefono) }
                                    )
                                }
                                val yape = tienda?.numeroYapePlin.orEmpty()
                                if (!estaBloqueada && yape.isNotBlank()) {
                                    InfoDetailRow(
                                        icon = Icons.Default.Payments,
                                        tint = palette.accentWarmGold,
                                        etiqueta = "Número Yape / Plin",
                                        valor = yape,
                                        onCopy = { copiar("Yape/Plin", yape) }
                                    )
                                }
                            }
                        }

                        if (!estaBloqueada && objetivo.productosTienda.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "PRODUCTOS DE $tituloNombre (${objetivo.productosTienda.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(objetivo.productosTienda, key = { it.id }) { prod ->
                                        val prodPress = rememberPressInteraction()
                                        Surface(
                                            onClick = { onAbrirProducto(prod) },
                                            interactionSource = prodPress,
                                            shape = RoundedCornerShape(16.dp),
                                            color = palette.barSurface,
                                            border = BorderStroke(1.dp, palette.barDivider),
                                            modifier = Modifier
                                                .width(152.dp)
                                                .pressScale(prodPress)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                ProductThumbnail(
                                                    imageUrl = prod.imagenUrl,
                                                    categoria = prod.nombreCategoria,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(92.dp)
                                                )
                                                Spacer(Modifier.height(7.dp))
                                                Text(
                                                    text = PiuraConstants.formatearSoles(prod.precio),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = palette.accentPrimary
                                                )
                                                Text(
                                                    text = prod.titulo,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is PerfilChatObjetivo.CompradorPerfil -> {
                        val buyer = objetivo.comprador
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = palette.barSurface,
                            border = BorderStroke(1.dp, palette.barDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "INFORMACIÓN PÚBLICA DEL COMPRADOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                InfoDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    tint = Color(0xFF0D9488),
                                    etiqueta = "Zona frecuente en Piura",
                                    valor = buyer?.zonaPiura?.ifBlank { "Piura / UNP" } ?: "Piura / UNP"
                                )
                                if (!estaBloqueada && telefono.isNotBlank()) {
                                    InfoDetailRow(
                                        icon = Icons.Default.Phone,
                                        tint = palette.onlineDot,
                                        etiqueta = "Teléfono de coordinación",
                                        valor = telefono,
                                        onCopy = { copiar("Teléfono", telefono) }
                                    )
                                }
                                InfoDetailRow(
                                    icon = Icons.Default.Verified,
                                    tint = Color(0xFF15803D),
                                    etiqueta = "Entregas completadas contigo",
                                    valor = "${objetivo.entregasConTienda} con PIN verificado"
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.inputSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = palette.accentPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Por privacidad, el DNI y los datos biométricos nunca se muestran en el perfil público.",
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Seccion de privacidad y seguridad de la conversacion
                if (conversacion != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = palette.barSurface,
                        border = BorderStroke(1.dp, palette.barDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PRIVACIDAD DE LA CONVERSACIÓN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            if (!estaBloqueada) {
                                val blockPress = rememberPressInteraction()
                                Surface(
                                    onClick = { mostrarConfirmacionBloqueo = true },
                                    interactionSource = blockPress,
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale(blockPress)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                                        Text("Bloquear conversación", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            } else if (bloqueadaPorMi) {
                                val unlockPress = rememberPressInteraction()
                                Surface(
                                    onClick = onDesbloquear,
                                    interactionSource = unlockPress,
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.accentPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale(unlockPress)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.LockOpen, null, tint = palette.accentPrimary, modifier = Modifier.size(19.dp))
                                        Text("Desbloquear conversación", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = palette.accentPrimary)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Por el momento no es posible enviar mensajes en esta conversación.",
                                    fontSize = 12.5.sp,
                                    color = palette.incomingMeta,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            val delPress = rememberPressInteraction()
                            Surface(
                                onClick = { mostrarConfirmacionEliminar = true },
                                interactionSource = delPress,
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(delPress)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                                    Text("Eliminar conversación", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarConfirmacionBloqueo) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBloqueo = false },
            icon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Bloquear a $tituloNombre?", fontWeight = FontWeight.Bold) },
            text = { Text("Se suspenderán los mensajes y los pedidos entre ambas partes hasta que desbloquees la conversación.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionBloqueo = false
                        onBloquear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Bloquear") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionBloqueo = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar conversación?", fontWeight = FontWeight.Bold) },
            text = { Text("Se borrarán de forma permanente todos los mensajes con $tituloNombre.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionEliminar = false
                        onEliminarConversacion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun QuickContactActionTile(
    icon: ImageVector,
    tint: Color,
    label: String,
    palette: EmprendeChatPalette,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val press = rememberPressInteraction()
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = press,
        shape = RoundedCornerShape(16.dp),
        color = palette.barSurface,
        border = BorderStroke(1.dp, palette.barDivider),
        modifier = modifier.pressScale(press)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else tint.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoDetailRow(
    icon: ImageVector,
    tint: Color,
    etiqueta: String,
    valor: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            Column {
                Text(etiqueta, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valor, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar $etiqueta", modifier = Modifier.size(18.dp))
            }
        }
    }
}
