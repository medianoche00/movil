package com.unp.emprendepiura.features.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
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
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.domain.model.Conversacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    state: ChatUiState,
    miUsuarioId: String,
    darkTheme: Boolean,
    onRefresh: () -> Unit,
    onAbrirConversacion: (Conversacion) -> Unit,
    onAbrirPerfilContacto: (Conversacion) -> Unit,
    onBloquearConversacion: (String) -> Unit,
    onDesbloquearConversacion: (String) -> Unit,
    onEliminarConversacion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberEmprendeChatPalette(darkTheme)
    var busqueda by remember { mutableStateOf("") }

    var conversacionAEliminar by remember { mutableStateOf<Conversacion?>(null) }
    var conversacionABloquear by remember { mutableStateOf<Conversacion?>(null) }

    val conversacionesFiltradas = remember(state.conversaciones, busqueda) {
        if (busqueda.isBlank()) {
            state.conversaciones
        } else {
            val q = busqueda.trim().lowercase()
            state.conversaciones.filter {
                it.nombreContraparte.lowercase().contains(q) ||
                    ChatMessageCodec.resumenLegible(it.ultimoMensaje).lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasBackground)
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.barSurface),
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Mensajes", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        StatusPillBadge(texto = "Tiempo real", color = palette.accentPrimary)
                    }
                    Text(
                        text = "Chats guardados con emprendedores y compradores",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar conversaciones")
                }
            }
        )

        Surface(
            color = palette.barSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = {
                        Text(
                            text = "Buscar conversación o mensaje...",
                            fontSize = 14.sp,
                            color = palette.inputPlaceholder
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = palette.inputPlaceholder,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = busqueda.isNotEmpty(),
                            enter = scaleIn(appleBouncySpring()) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            IconButton(onClick = { busqueda = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentPrimary,
                        unfocusedBorderColor = palette.inputBorder,
                        focusedContainerColor = palette.barSurface,
                        unfocusedContainerColor = palette.inputSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = palette.barDivider.copy(alpha = 0.7f))
            }
        }

        val modoLista = when {
            state.loadingConversaciones && state.conversaciones.isEmpty() -> 0
            conversacionesFiltradas.isEmpty() -> 1
            else -> 2
        }

        AnimatedContent(
            targetState = modoLista,
            transitionSpec = {
                fadeIn(tween(230, easing = AppleNavigationEasing)) togetherWith fadeOut(tween(150))
            },
            modifier = Modifier.weight(1f),
            label = "chatsListMode"
        ) { modo ->
            when (modo) {
                0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.accentPrimary)
                    }
                }

                1 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateBox(
                            icon = if (busqueda.isNotBlank()) Icons.Default.Search else Icons.AutoMirrored.Filled.Chat,
                            titulo = if (busqueda.isNotBlank()) "Sin resultados" else "Aún no tienes conversaciones",
                            subtitulo = if (busqueda.isNotBlank()) {
                                "No encontramos chats para \"$busqueda\"."
                            } else {
                                "Entra a cualquier producto o pedido y toca Mensaje para coordinar en tiempo real."
                            }
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(conversacionesFiltradas, key = { it.id }) { conv ->
                            ConversacionListCard(
                                conv = conv,
                                miUsuarioId = miUsuarioId,
                                palette = palette,
                                onAbrir = { onAbrirConversacion(conv) },
                                onVerPerfil = { onAbrirPerfilContacto(conv) },
                                onSolicitarBloqueo = { conversacionABloquear = conv },
                                onDesbloquear = { onDesbloquearConversacion(conv.id) },
                                onSolicitarEliminar = { conversacionAEliminar = conv },
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

    if (conversacionABloquear != null) {
        val target = conversacionABloquear!!
        AlertDialog(
            onDismissRequest = { conversacionABloquear = null },
            icon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Bloquear conversación?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Se suspenderán los mensajes y los pedidos con ${target.nombreContraparte} hasta que desbloquees esta conversación.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        conversacionABloquear = null
                        onBloquearConversacion(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversacionABloquear = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (conversacionAEliminar != null) {
        val target = conversacionAEliminar!!
        AlertDialog(
            onDismissRequest = { conversacionAEliminar = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar conversación?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Se borrarán de forma permanente todos los mensajes con ${target.nombreContraparte}. Esta acción no se puede deshacer.")
            },
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
}

@Composable
private fun ConversacionListCard(
    conv: Conversacion,
    miUsuarioId: String,
    palette: EmprendeChatPalette,
    onAbrir: () -> Unit,
    onVerPerfil: () -> Unit,
    onSolicitarBloqueo: () -> Unit,
    onDesbloquear: () -> Unit,
    onSolicitarEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val press = rememberPressInteraction()
    var menuAbierto by remember { mutableStateOf(false) }

    val bloqueada = conv.estaBloqueada
    val bloqueadaPorMi = conv.bloqueadaPorMi(miUsuarioId)
    val hora = formatearHoraLocalChat(conv.actualizadoEn)

    Surface(
        onClick = onAbrir,
        interactionSource = press,
        shape = RoundedCornerShape(18.dp),
        color = palette.barSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (conv.mensajesNoLeidos > 0 && !bloqueada) {
                palette.accentPrimary.copy(alpha = 0.35f)
            } else {
                palette.barDivider.copy(alpha = 0.75f)
            }
        ),
        shadowElevation = if (palette.isDark) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(press)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (bloqueada) MaterialTheme.colorScheme.surfaceVariant
                            else palette.avatarBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (bloqueada && bloqueadaPorMi) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = conv.nombreContraparte.take(1).uppercase().ifBlank { "E" },
                            color = if (bloqueada) MaterialTheme.colorScheme.onSurfaceVariant else palette.avatarText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conv.nombreContraparte.ifBlank { "Conversación" },
                        fontWeight = if (conv.mensajesNoLeidos > 0) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (hora.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = hora,
                            fontSize = 11.sp,
                            fontWeight = if (conv.mensajesNoLeidos > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (conv.mensajesNoLeidos > 0) palette.accentPrimary else palette.incomingMeta
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (bloqueada) {
                        StatusPillBadge(
                            texto = if (bloqueadaPorMi) "Bloqueado por ti" else "No disponible",
                            color = if (bloqueadaPorMi) MaterialTheme.colorScheme.error else palette.incomingMeta
                        )
                    } else {
                        val preview = remember(conv.ultimoMensaje) {
                            ChatMessageCodec.resumenLegible(conv.ultimoMensaje).ifBlank { "Conversación iniciada" }
                        }
                        Text(
                            text = preview,
                            fontSize = 13.sp,
                            fontWeight = if (conv.mensajesNoLeidos > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (conv.mensajesNoLeidos > 0) MaterialTheme.colorScheme.onSurface else palette.incomingMeta,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(
                        visible = conv.mensajesNoLeidos > 0 && !bloqueada,
                        enter = scaleIn(appleBouncySpring()) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(palette.unreadBadgeBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conv.mensajesNoLeidos > 99) "99+" else "${conv.mensajesNoLeidos}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.unreadBadgeText
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones de conversación",
                        tint = palette.incomingMeta
                    )
                }

                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver perfil del contacto") },
                        leadingIcon = { Icon(Icons.Default.Storefront, null) },
                        onClick = {
                            menuAbierto = false
                            onVerPerfil()
                        }
                    )

                    if (!bloqueada) {
                        DropdownMenuItem(
                            text = { Text("Bloquear chat") },
                            leadingIcon = { Icon(Icons.Default.Block, null) },
                            onClick = {
                                menuAbierto = false
                                onSolicitarBloqueo()
                            }
                        )
                    } else if (bloqueadaPorMi) {
                        DropdownMenuItem(
                            text = { Text("Desbloquear chat") },
                            leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                            onClick = {
                                menuAbierto = false
                                onDesbloquear()
                            }
                        )
                    }

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Eliminar conversación", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuAbierto = false
                            onSolicitarEliminar()
                        }
                    )
                }
            }
        }
    }
}
