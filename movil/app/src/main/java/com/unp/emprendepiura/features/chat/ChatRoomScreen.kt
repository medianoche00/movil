package com.unp.emprendepiura.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Mensaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversacion: Conversacion,
    mensajes: List<Mensaje>,
    tiendaContraparte: Emprendimiento? = null,
    miUsuarioId: String,
    loading: Boolean,
    enviando: Boolean,
    contraparteEscribiendo: Boolean = false,
    estaBloqueado: Boolean = false,
    onEnviar: (String) -> Unit,
    onBloquear: () -> Unit = {},
    onDesbloquear: () -> Unit = {},
    onEliminarConversacion: () -> Unit = {},
    onVolver: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var menuOpcionesAbierto by remember { mutableStateOf(false) }
    var verPerfilNegocio by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }
    var mostrarConfirmacionBloqueo by remember { mutableStateOf(false) }

    LaunchedEffect(mensajes.size, contraparteEscribiendo) {
        if (mensajes.isNotEmpty() || contraparteEscribiendo) {
            val destino = (mensajes.size - 1) + (if (contraparteEscribiendo) 1 else 0)
            if (destino >= 0) listState.animateScrollToItem(destino)
        }
    }

    val bgColor = if (isDark) WhatsAppBgDark else WhatsAppBgLight
    val nombreTitulo = conversacion.nombreContraparte.ifBlank { "Emprendimiento Piura" }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF1F2C34) else MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { verPerfilNegocio = true }
                    ) {
                        // Avatar circular
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = nombreTitulo.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nombreTitulo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            if (contraparteEscribiendo) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "escribiendo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WhatsAppGreenPrimary
                                    )
                                    TypingIndicatorDots(tamanoPunto = 4f, puntoColor = WhatsAppGreenPrimary)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(WhatsAppGreenPrimary)
                                    )
                                    Text(
                                        text = "En línea · Toca para ver perfil",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpcionesAbierto = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuOpcionesAbierto,
                            onDismissRequest = { menuOpcionesAbierto = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ver perfil del negocio") },
                                leadingIcon = { Icon(Icons.Default.Storefront, null) },
                                onClick = {
                                    menuOpcionesAbierto = false
                                    verPerfilNegocio = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (estaBloqueado) "Desbloquear" else "Bloquear") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (estaBloqueado) Icons.Default.LockOpen else Icons.Default.Block,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuOpcionesAbierto = false
                                    if (estaBloqueado) onDesbloquear()
                                    else mostrarConfirmacionBloqueo = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Eliminar conversación", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuOpcionesAbierto = false
                                    mostrarConfirmacionEliminar = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = if (isDark) Color(0xFF1F2C34) else MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                if (estaBloqueado) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Has bloqueado esta conversación.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onDesbloquear,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Desbloquear", fontSize = 12.5.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Input redondeado estilo WhatsApp / Telegram
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = if (isDark) Color(0xFF2A3942) else Color.White,
                            shadowElevation = 1.5.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = texto,
                                    onValueChange = { texto = it },
                                    placeholder = {
                                        Text(
                                            "Mensaje...",
                                            fontSize = 15.sp,
                                            color = if (isDark) Color(0xFF8696A0) else Color(0xFF8696A0)
                                        )
                                    },
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Botón de envío dinámico estilo WhatsApp / Telegram
                        FilledIconButton(
                            onClick = {
                                if (texto.isNotBlank()) {
                                    onEnviar(texto)
                                    texto = ""
                                }
                            },
                            enabled = texto.isNotBlank() && !enviando,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = WhatsAppGreenPrimary,
                                disabledContainerColor = WhatsAppGreenPrimary.copy(alpha = 0.4f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(bgColor)
        ) {
            if (loading && mensajes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WhatsAppGreenPrimary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        ChatDateBadge(texto = "Conversación en vivo · EmprendePiura")
                    }

                    items(mensajes, key = { it.id.ifBlank { it.creadoEn + it.contenido } }) { msg ->
                        val esMio = msg.remitenteId == miUsuarioId
                        WhatsAppChatBubble(mensaje = msg, esMio = esMio)
                    }

                    if (contraparteEscribiendo) {
                        item {
                            TypingBubbleMessage(nombreEmprendedor = nombreTitulo)
                        }
                    }
                }
            }
        }
    }

    // Modal de Perfil de Negocio
    if (verPerfilNegocio) {
        PerfilNegocioDialog(
            tienda = tiendaContraparte,
            nombreFallback = nombreTitulo,
            onDismiss = { verPerfilNegocio = false }
        )
    }

    // Dialogo confirmacion de eliminar chat
    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            title = { Text("¿Eliminar conversación?", fontWeight = FontWeight.Bold) },
            text = { Text("Se borrarán todos los mensajes de esta conversación con $nombreTitulo. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionEliminar = false
                        onEliminarConversacion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialogo confirmacion de bloqueo
    if (mostrarConfirmacionBloqueo) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBloqueo = false },
            title = { Text("¿Bloquear a $nombreTitulo?", fontWeight = FontWeight.Bold) },
            text = { Text("No podrás enviar ni recibir nuevos mensajes de este usuario o tienda.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionBloqueo = false
                        onBloquear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bloquear")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionBloqueo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
