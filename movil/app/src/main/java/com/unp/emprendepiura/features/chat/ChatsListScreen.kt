package com.unp.emprendepiura.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    state: ChatUiState,
    tiendas: Map<String, Emprendimiento> = emptyMap(),
    onRefresh: () -> Unit,
    onAbrirConversacion: (Conversacion) -> Unit,
    onBloquearConversacion: (String) -> Unit = {},
    onDesbloquearConversacion: (String) -> Unit = {},
    onEliminarConversacion: (String) -> Unit = {},
    onVerPerfilTienda: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var busqueda by remember { mutableStateOf("") }

    // Dialogos locales
    var conversacionAEliminar by remember { mutableStateOf<Conversacion?>(null) }
    var verPerfilTiendaId by remember { mutableStateOf<Pair<String, String>?>(null) }

    val conversacionesFiltradas = remember(state.conversaciones, busqueda) {
        if (busqueda.isBlank()) state.conversaciones
        else {
            val q = busqueda.trim().lowercase()
            state.conversaciones.filter {
                it.nombreContraparte.lowercase().contains(q) ||
                        it.ultimoMensaje.lowercase().contains(q)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Mensajes", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WhatsAppGreenPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "En Vivo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreenPrimary
                            )
                        }
                    }
                    Text(
                        text = "Mensajería en tiempo real estilo WhatsApp / Telegram",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar chats")
                }
            }
        )

        // Barra de búsqueda rápida estilo WhatsApp / Telegram
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1F2C34) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar chat o emprendimiento...", fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (busqueda.isNotBlank()) {
                    IconButton(onClick = { busqueda = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        when {
            state.loadingConversaciones && state.conversaciones.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WhatsAppGreenPrimary)
                }
            }

            state.conversaciones.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateBox(
                        icon = Icons.Default.ChatBubbleOutline,
                        titulo = "Sin conversaciones activas",
                        subtitulo = "Entra a cualquier producto y toca Mensaje para chatear en tiempo real con el emprendedor."
                    )
                }
            }

            conversacionesFiltradas.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateBox(
                        icon = Icons.Default.SearchOff,
                        titulo = "No se encontraron chats",
                        subtitulo = "No hay resultados para '$busqueda'."
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversacionesFiltradas, key = { it.id }) { conv ->
                        val estaBloqueado = state.conversacionesBloqueadas.contains(conv.id)
                        var menuFilaAbierto by remember { mutableStateOf(false) }

                        val hora = conv.actualizadoEn.drop(11).take(5).ifBlank { "" }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAbrirConversacion(conv) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            // Avatar con letra y badge verde online
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (estaBloqueado) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.primaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conv.nombreContraparte.take(1).uppercase().ifBlank { "C" },
                                        color = if (estaBloqueado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    )
                                }
                                if (!estaBloqueado) {
                                    Box(
                                        modifier = Modifier
                                            .size(13.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(1.5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(WhatsAppGreenPrimary)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.nombreContraparte.ifBlank { "Conversación" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (hora.isNotBlank()) {
                                        Text(
                                            text = hora,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (estaBloqueado) {
                                        StatusPillBadge("Bloqueado", MaterialTheme.colorScheme.error)
                                    } else {
                                        Text(
                                            text = conv.ultimoMensaje.ifBlank { "Conversación iniciada" },
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Botón menú contextual de la conversación
                            Box {
                                IconButton(onClick = { menuFilaAbierto = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Opciones del chat",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuFilaAbierto,
                                    onDismissRequest = { menuFilaAbierto = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ver perfil del negocio") },
                                        leadingIcon = { Icon(Icons.Default.Storefront, null) },
                                        onClick = {
                                            menuFilaAbierto = false
                                            if (onVerPerfilTienda != null) {
                                                onVerPerfilTienda(conv.emprendimientoId, conv.nombreContraparte)
                                            } else {
                                                verPerfilTiendaId = conv.emprendimientoId to conv.nombreContraparte
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (estaBloqueado) "Desbloquear chat" else "Bloquear chat") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (estaBloqueado) Icons.Default.LockOpen else Icons.Default.Block,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            menuFilaAbierto = false
                                            if (estaBloqueado) onDesbloquearConversacion(conv.id)
                                            else onBloquearConversacion(conv.id)
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Eliminar conversación", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuFilaAbierto = false
                                            conversacionAEliminar = conv
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(start = 80.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal de confirmación para eliminar chat
    if (conversacionAEliminar != null) {
        val target = conversacionAEliminar!!
        AlertDialog(
            onDismissRequest = { conversacionAEliminar = null },
            title = { Text("¿Eliminar conversación?", fontWeight = FontWeight.Bold) },
            text = { Text("Se borrarán todos los mensajes del chat con ${target.nombreContraparte}. Esta acción es definitiva.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        conversacionAEliminar = null
                        onEliminarConversacion(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversacionAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Perfil de Negocio
    if (verPerfilTiendaId != null) {
        val (tiendaId, nombre) = verPerfilTiendaId!!
        PerfilNegocioDialog(
            tienda = state.tiendaContraparte?.takeIf { it.id == tiendaId } ?: tiendas[tiendaId],
            nombreFallback = nombre,
            onDismiss = { verPerfilTiendaId = null }
        )
    }
}
