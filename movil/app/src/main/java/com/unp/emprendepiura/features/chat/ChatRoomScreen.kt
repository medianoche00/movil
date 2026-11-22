package com.unp.emprendepiura.features.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
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
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Mensaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversacion: Conversacion,
    mensajes: List<Mensaje>,
    tiendaContraparte: Emprendimiento?,
    miUsuarioId: String,
    darkTheme: Boolean,
    loading: Boolean,
    enviando: Boolean,
    contraparteEnLinea: Boolean,
    contraparteEscribiendo: Boolean,
    onNotificarEscritura: (Boolean) -> Unit,
    onAbrirPerfilContacto: () -> Unit,
    onEnviar: (String) -> Unit,
    onBloquear: () -> Unit,
    onDesbloquear: () -> Unit,
    onEliminarConversacion: () -> Unit,
    onVolver: () -> Unit
) {
    val palette = rememberEmprendeChatPalette(darkTheme)
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var menuOpcionesAbierto by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }
    var mostrarConfirmacionBloqueo by remember { mutableStateOf(false) }

    val estaBloqueada = conversacion.estaBloqueada
    val bloqueadaPorMi = conversacion.bloqueadaPorMi(miUsuarioId)
    val nombreTitulo = conversacion.nombreContraparte.ifBlank { "Emprendimiento Piura" }

    DisposableEffect(conversacion.id) {
        onDispose { onNotificarEscritura(false) }
    }

    LaunchedEffect(mensajes.size, contraparteEscribiendo) {
        val totalItems = mensajes.size + (if (contraparteEscribiendo && !estaBloqueada) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        containerColor = palette.canvasBackground,
        topBar = {
            Surface(
                color = palette.barSurface,
                shadowElevation = if (palette.isDark) 0.dp else 2.dp
            ) {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = palette.barSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        navigationIcon = {
                            IconButton(onClick = onVolver) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onAbrirPerfilContacto() }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(palette.avatarBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = nombreTitulo.take(1).uppercase(),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = palette.avatarText
                                        )
                                    }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = contraparteEnLinea && !estaBloqueada,
                                        enter = scaleIn(appleBouncySpring()) + fadeIn(),
                                        exit = scaleOut() + fadeOut(),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(palette.barSurface)
                                                .padding(2.dp)
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

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nombreTitulo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val estadoHeader = when {
                                        estaBloqueada -> 0
                                        contraparteEscribiendo -> 1
                                        contraparteEnLinea -> 2
                                        else -> 3
                                    }

                                    AnimatedContent(
                                        targetState = estadoHeader,
                                        transitionSpec = {
                                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                        },
                                        label = "chatHeaderStatus"
                                    ) { st ->
                                        when (st) {
                                            0 -> Text(
                                                text = if (bloqueadaPorMi) "Bloqueado por ti" else "Conversación bloqueada",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            1 -> Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = "escribiendo",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = palette.accentPrimary
                                                )
                                                TypingIndicatorDots(puntoColor = palette.accentPrimary, tamanoPunto = 3.8f)
                                            }
                                            2 -> Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.5.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.onlineDot)
                                                )
                                                Text(
                                                    text = "En línea ahora",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = palette.onlineDot
                                                )
                                            }
                                            else -> Text(
                                                text = "${tiendaContraparte?.zonaPiura ?: "EmprendePiura"} · Toca para ver perfil",
                                                fontSize = 11.5.sp,
                                                color = palette.incomingMeta,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
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
                                        text = { Text("Ver perfil completo") },
                                        leadingIcon = { Icon(Icons.Default.Storefront, null) },
                                        onClick = {
                                            menuOpcionesAbierto = false
                                            onAbrirPerfilContacto()
                                        }
                                    )
                                    if (!estaBloqueada) {
                                        DropdownMenuItem(
                                            text = { Text("Bloquear conversación") },
                                            leadingIcon = { Icon(Icons.Default.Block, null) },
                                            onClick = {
                                                menuOpcionesAbierto = false
                                                mostrarConfirmacionBloqueo = true
                                            }
                                        )
                                    } else if (bloqueadaPorMi) {
                                        DropdownMenuItem(
                                            text = { Text("Desbloquear conversación") },
                                            leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                                            onClick = {
                                                menuOpcionesAbierto = false
                                                onDesbloquear()
                                            }
                                        )
                                    }
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
                    HorizontalDivider(color = palette.barDivider)
                }
            }
        },
        bottomBar = {
            Surface(
                color = palette.barSurface,
                shadowElevation = if (palette.isDark) 0.dp else 6.dp
            ) {
                Column {
                    HorizontalDivider(color = palette.barDivider)
                    AnimatedContent(
                        targetState = estaBloqueada,
                        transitionSpec = {
                            (fadeIn(tween(220, easing = AppleNavigationEasing)) + expandVertically()) togetherWith
                                (fadeOut(tween(150)) + shrinkVertically())
                        },
                        label = "chatComposerMode"
                    ) { blocked ->
                        if (blocked) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (bloqueadaPorMi) {
                                            "Bloqueaste esta conversación."
                                        } else {
                                            "Esta conversación fue bloqueada por la contraparte."
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (bloqueadaPorMi) {
                                    Spacer(Modifier.width(10.dp))
                                    val unlockPress = rememberPressInteraction()
                                    Button(
                                        onClick = onDesbloquear,
                                        interactionSource = unlockPress,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary),
                                        modifier = Modifier.pressScale(unlockPress)
                                    ) {
                                        Text("Desbloquear", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .imePadding()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = palette.inputSurface,
                                    border = BorderStroke(1.dp, palette.inputBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = texto,
                                        onValueChange = { nuevo ->
                                            texto = nuevo
                                            onNotificarEscritura(nuevo.isNotBlank())
                                        },
                                        placeholder = {
                                            Text(
                                                text = "Escribe un mensaje...",
                                                fontSize = 14.5.sp,
                                                color = palette.inputPlaceholder
                                            )
                                        },
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp)
                                    )
                                }

                                val puedeEnviar = texto.isNotBlank() && !enviando
                                val sendBg by animateColorAsState(
                                    targetValue = if (puedeEnviar) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.35f),
                                    animationSpec = tween(200),
                                    label = "sendBtnColor"
                                )
                                val sendPress = rememberPressInteraction()
                                FilledIconButton(
                                    onClick = {
                                        if (puedeEnviar) {
                                            val contenido = texto
                                            texto = ""
                                            onNotificarEscritura(false)
                                            onEnviar(contenido)
                                        }
                                    },
                                    enabled = puedeEnviar,
                                    interactionSource = sendPress,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = sendBg,
                                        disabledContainerColor = sendBg,
                                        contentColor = Color.White,
                                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .pressScale(sendPress)
                                ) {
                                    if (enviando) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Enviar mensaje",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
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
                .background(palette.canvasBackground)
        ) {
            if (loading && mensajes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accentPrimary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (mensajes.isEmpty()) {
                        item(key = "chat_empty_banner") {
                            ChatDateBadge(
                                texto = "Conversación guardada en tiempo real · EmprendePiura",
                                palette = palette
                            )
                        }
                    }

                    itemsIndexed(
                        items = mensajes,
                        key = { _, m -> m.id.ifBlank { "${m.creadoEn}_${m.contenido}" } }
                    ) { index, msg ->
                        val diaActual = claveDiaChat(msg.creadoEn)
                        val diaPrevio = if (index > 0) claveDiaChat(mensajes[index - 1].creadoEn) else null

                        Column(
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(220),
                                placementSpec = applePushSpring()
                            )
                        ) {
                            if (index == 0 || diaActual != diaPrevio) {
                                ChatDateBadge(
                                    texto = formatearEtiquetaDiaChat(msg.creadoEn),
                                    palette = palette
                                )
                            }
                            EmprendeChatBubble(
                                mensaje = msg,
                                esMio = msg.remitenteId == miUsuarioId,
                                palette = palette
                            )
                        }
                    }

                    if (contraparteEscribiendo && !estaBloqueada) {
                        item(key = "typing_live_bubble") {
                            TypingBubbleMessage(
                                nombreContraparte = nombreTitulo,
                                palette = palette,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(200),
                                    placementSpec = applePushSpring()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar conversación?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Se borrarán de forma permanente todos los mensajes con $nombreTitulo. Esta acción no se puede deshacer.")
            },
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

    if (mostrarConfirmacionBloqueo) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBloqueo = false },
            icon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Bloquear a $nombreTitulo?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Esta conversación quedará bloqueada para ambas partes y no se podrán enviar mensajes hasta que la desbloquees.")
            },
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
