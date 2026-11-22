package com.unp.emprendepiura.features.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Mensaje
import kotlinx.coroutines.delay

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
    enviandoAdjunto: Boolean,
    etiquetaAdjuntoEnProgreso: String?,
    contraparteEnLinea: Boolean,
    contraparteEscribiendo: Boolean,
    onNotificarEscritura: (Boolean) -> Unit,
    onAbrirPerfilContacto: () -> Unit,
    onEnviarTexto: (String, ChatReplyQuote?) -> Unit,
    onEnviarImagen: (Uri, ChatReplyQuote?) -> Unit,
    onEnviarAudio: (ByteArray, Int, ChatReplyQuote?) -> Unit,
    onEnviarDocumento: (Uri, ChatReplyQuote?) -> Unit,
    onEnviarUbicacion: (ChatReplyQuote?) -> Unit,
    onEnviarContacto: (nombre: String, telefono: String, reply: ChatReplyQuote?) -> Unit,
    onEliminarMensajeParaMi: (Mensaje) -> Unit,
    onEliminarMensajeParaTodos: (Mensaje) -> Unit,
    onMostrarAviso: (String) -> Unit,
    onBloquear: () -> Unit,
    onDesbloquear: () -> Unit,
    onEliminarConversacion: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val palette = rememberEmprendeChatPalette(darkTheme)
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var menuOpcionesAbierto by remember { mutableStateOf(false) }
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }
    var mostrarConfirmacionBloqueo by remember { mutableStateOf(false) }

    var panelAdjuntosAbierto by remember { mutableStateOf(false) }
    var mensajeAResponder by remember { mutableStateOf<ChatReplyQuote?>(null) }
    var mensajeOpcionesActivo by remember { mutableStateOf<Mensaje?>(null) }
    var imagenPantallaCompletaUrl by remember { mutableStateOf<String?>(null) }
    var uriFotoPendienteCamara by remember { mutableStateOf<Uri?>(null) }

    // Grabador y reproductor de audio nativos con ciclo de vida controlado
    val audioRecorder = remember { ChatAudioRecorder() }
    val audioPlayer = remember { ChatAudioPlayerController() }
    val audioPlaybackState by audioPlayer.state.collectAsState()

    var grabandoAudio by remember { mutableStateOf(false) }
    var segundosGrabacion by remember { mutableIntStateOf(0) }
    var nivelesAudio by remember { mutableStateOf(List(18) { 0.2f }) }

    val estaBloqueada = conversacion.estaBloqueada
    val bloqueadaPorMi = conversacion.bloqueadaPorMi(miUsuarioId)
    val nombreTitulo = conversacion.nombreContraparte.ifBlank { "Emprendimiento Piura" }

    // --- Launchers nativos de adjuntos y permisos bajo demanda ---
    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val reply = mensajeAResponder
            mensajeAResponder = null
            onEnviarImagen(uri, reply)
        }
    }

    val camaraFotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = uriFotoPendienteCamara
        uriFotoPendienteCamara = null
        if (ok && uri != null) {
            val reply = mensajeAResponder
            mensajeAResponder = null
            onEnviarImagen(uri, reply)
        }
    }

    fun abrirCamaraChat() {
        runCatching {
            val uri = ChatDeviceHelper.crearUriFotoTemporalCamara(context)
            uriFotoPendienteCamara = uri
            camaraFotoLauncher.launch(uri)
        }.onFailure {
            onMostrarAviso("No se pudo abrir la cámara del dispositivo.")
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamaraChat()
        else onMostrarAviso("Se requiere permiso de cámara para tomar y enviar fotos.")
    }

    val documentoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val reply = mensajeAResponder
            mensajeAResponder = null
            onEnviarDocumento(uri, reply)
        }
    }

    val contactoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        if (contactUri != null) {
            val datos = ChatDeviceHelper.extraerContactoDesdeUri(context, contactUri)
            if (datos != null) {
                val reply = mensajeAResponder
                mensajeAResponder = null
                onEnviarContacto(datos.first, datos.second, reply)
            } else {
                onMostrarAviso("No se pudo leer el contacto seleccionado.")
            }
        }
    }

    val permisoContactosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactoLauncher.launch(null)
        else onMostrarAviso("Se requiere permiso de contactos para compartir un contacto.")
    }

    val permisoUbicacionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val concedido = resultados[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            resultados[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            val reply = mensajeAResponder
            mensajeAResponder = null
            onEnviarUbicacion(reply)
        } else {
            onMostrarAviso("Se requiere permiso de ubicación para compartir tu ubicación GPS.")
        }
    }

    fun iniciarGrabacionNotaVoz() {
        audioPlayer.detenerActual()
        panelAdjuntosAbierto = false
        val iniciado = audioRecorder.iniciar(context)
        if (iniciado) {
            segundosGrabacion = 0
            nivelesAudio = List(18) { 0.2f }
            grabandoAudio = true
        } else {
            onMostrarAviso("No se pudo iniciar la grabación de audio.")
        }
    }

    val permisoAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) iniciarGrabacionNotaVoz()
        else onMostrarAviso("Se requiere permiso de micrófono para grabar notas de voz.")
    }

    fun solicitarCamara() {
        panelAdjuntosAbierto = false
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) abrirCamaraChat() else permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
    }

    fun solicitarUbicacion() {
        panelAdjuntosAbierto = false
        if (ChatDeviceHelper.tienePermisoUbicacion(context)) {
            val reply = mensajeAResponder
            mensajeAResponder = null
            onEnviarUbicacion(reply)
        } else {
            permisoUbicacionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun solicitarContacto() {
        panelAdjuntosAbierto = false
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) contactoLauncher.launch(null) else permisoContactosLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    fun solicitarAudio() {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) iniciarGrabacionNotaVoz() else permisoAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(grabandoAudio) {
        if (grabandoAudio) {
            val inicio = System.currentTimeMillis()
            while (grabandoAudio) {
                segundosGrabacion = ((System.currentTimeMillis() - inicio) / 1000L).toInt()
                val nivel = audioRecorder.obtenerNivelAmplitud()
                nivelesAudio = (nivelesAudio.drop(1) + nivel)
                delay(180L)
            }
        }
    }

    LaunchedEffect(estaBloqueada) {
        if (estaBloqueada) {
            panelAdjuntosAbierto = false
            mensajeAResponder = null
            if (grabandoAudio) {
                audioRecorder.cancelar()
                grabandoAudio = false
            }
        }
    }

    DisposableEffect(conversacion.id) {
        onDispose {
            onNotificarEscritura(false)
            audioRecorder.cancelar()
            audioPlayer.liberar()
        }
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
                                                text = if (bloqueadaPorMi) "Bloqueado por ti" else "No disponible",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (bloqueadaPorMi) MaterialTheme.colorScheme.error else palette.incomingMeta
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

                    // Indicador discreto de subida de adjunto (imagen, audio, documento o GPS)
                    AnimatedVisibility(
                        visible = enviandoAdjunto && !etiquetaAdjuntoEnProgreso.isNullOrBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.accentPrimary.copy(alpha = 0.09f))
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = palette.accentPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = etiquetaAdjuntoEnProgreso.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accentPrimary
                            )
                        }
                    }

                    // Barra de respuesta citada (deslizar mensaje)
                    if (!estaBloqueada) {
                        ChatReplyPreviewBar(
                            quote = mensajeAResponder,
                            palette = palette,
                            onCancel = { mensajeAResponder = null }
                        )
                    }

                    val modoComposer = when {
                        estaBloqueada -> 0
                        grabandoAudio -> 1
                        else -> 2
                    }

                    AnimatedContent(
                        targetState = modoComposer,
                        transitionSpec = {
                            (fadeIn(tween(220, easing = AppleNavigationEasing)) + expandVertically()) togetherWith
                                (fadeOut(tween(150)) + shrinkVertically())
                        },
                        label = "chatComposerMode"
                    ) { modo ->
                        when (modo) {
                            0 -> {
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
                                            imageVector = if (bloqueadaPorMi) Icons.Default.Lock else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (bloqueadaPorMi) MaterialTheme.colorScheme.error else palette.incomingMeta,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (bloqueadaPorMi) {
                                                "Bloqueaste esta conversación."
                                            } else {
                                                "Por el momento no es posible enviar mensajes en esta conversación."
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
                            }

                            1 -> {
                                ChatAudioRecordingBar(
                                    duracionSeg = segundosGrabacion,
                                    niveles = nivelesAudio,
                                    palette = palette,
                                    onCancelar = {
                                        audioRecorder.cancelar()
                                        grabandoAudio = false
                                    },
                                    onEnviar = {
                                        grabandoAudio = false
                                        val resultado = audioRecorder.detenerYLeer()
                                        if (resultado != null) {
                                            val reply = mensajeAResponder
                                            mensajeAResponder = null
                                            onEnviarAudio(resultado.bytes, resultado.duracionSeg, reply)
                                        } else {
                                            onMostrarAviso("La nota de voz fue demasiado corta.")
                                        }
                                    }
                                )
                            }

                            else -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (!panelAdjuntosAbierto) Modifier.navigationBarsPadding() else Modifier)
                                        .imePadding()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(26.dp),
                                        color = palette.inputSurface,
                                        border = BorderStroke(1.dp, palette.inputBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = texto,
                                                onValueChange = { nuevo ->
                                                    texto = nuevo
                                                    if (nuevo.isNotEmpty() && panelAdjuntosAbierto) {
                                                        panelAdjuntosAbierto = false
                                                    }
                                                    onNotificarEscritura(nuevo.isNotBlank())
                                                },
                                                placeholder = {
                                                    Text(
                                                        text = "Mensaje",
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
                                                    .weight(1f)
                                                    .padding(start = 4.dp)
                                            )

                                            // Boton Clip (adjuntos)
                                            IconButton(
                                                onClick = { panelAdjuntosAbierto = !panelAdjuntosAbierto },
                                                enabled = !enviandoAdjunto,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachFile,
                                                    contentDescription = "Adjuntar archivo",
                                                    tint = if (panelAdjuntosAbierto) palette.accentPrimary else palette.incomingMeta,
                                                    modifier = Modifier
                                                        .size(21.dp)
                                                        .rotate(35f)
                                                )
                                            }

                                            // Acceso rapido a Camara cuando el campo esta vacio
                                            AnimatedVisibility(
                                                visible = texto.isBlank(),
                                                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f),
                                                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)
                                            ) {
                                                IconButton(
                                                    onClick = { solicitarCamara() },
                                                    enabled = !enviandoAdjunto,
                                                    modifier = Modifier
                                                        .padding(end = 4.dp)
                                                        .size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PhotoCamera,
                                                        contentDescription = "Tomar foto",
                                                        tint = palette.incomingMeta,
                                                        modifier = Modifier.size(21.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val hayTexto = texto.isNotBlank()
                                    val ocupado = enviando || enviandoAdjunto
                                    val btnBg by animateColorAsState(
                                        targetValue = if (ocupado) palette.accentPrimary.copy(alpha = 0.55f) else palette.accentPrimary,
                                        animationSpec = tween(200),
                                        label = "primaryActionBtnBg"
                                    )
                                    val actionPress = rememberPressInteraction()
                                    FilledIconButton(
                                        onClick = {
                                            if (ocupado) return@FilledIconButton
                                            if (hayTexto) {
                                                val contenido = texto
                                                val reply = mensajeAResponder
                                                texto = ""
                                                mensajeAResponder = null
                                                panelAdjuntosAbierto = false
                                                onNotificarEscritura(false)
                                                onEnviarTexto(contenido, reply)
                                            } else {
                                                solicitarAudio()
                                            }
                                        },
                                        enabled = !ocupado,
                                        interactionSource = actionPress,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = btnBg,
                                            disabledContainerColor = btnBg,
                                            contentColor = Color.White,
                                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                                        ),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .pressScale(actionPress)
                                    ) {
                                        if (enviando) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                color = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            AnimatedContent(
                                                targetState = hayTexto,
                                                transitionSpec = {
                                                    (scaleIn(appleBouncySpring()) + fadeIn(tween(150))) togetherWith
                                                        (scaleOut(targetScale = 0.75f) + fadeOut(tween(120)))
                                                },
                                                label = "sendOrMicIcon"
                                            ) { mostrarEnviar ->
                                                Icon(
                                                    imageVector = if (mostrarEnviar) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                                    contentDescription = if (mostrarEnviar) "Enviar mensaje" else "Grabar nota de voz",
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bandeja desplegable de adjuntos (Galeria, Camara, Ubicacion, Contacto, Documento)
                    ChatAttachmentTray(
                        visible = panelAdjuntosAbierto && !estaBloqueada && !grabandoAudio,
                        palette = palette,
                        onGaleria = {
                            panelAdjuntosAbierto = false
                            galeriaLauncher.launch("image/*")
                        },
                        onCamara = { solicitarCamara() },
                        onUbicacion = { solicitarUbicacion() },
                        onContacto = { solicitarContacto() },
                        onDocumento = {
                            panelAdjuntosAbierto = false
                            documentoLauncher.launch(arrayOf("*/*"))
                        }
                    )
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
                        val esMio = msg.remitenteId == miUsuarioId

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

                            val esEliminado = ChatMessageCodec.esEliminadoParaTodos(msg.contenido)

                            SwipeableReplyMessageContainer(
                                enabled = !estaBloqueada && !esEliminado,
                                palette = palette,
                                onResponder = {
                                    mensajeAResponder = ChatReplyQuote(
                                        messageId = msg.id,
                                        senderName = if (esMio) "Tú" else nombreTitulo,
                                        preview = ChatMessageCodec.resumenLegible(msg.contenido)
                                    )
                                }
                            ) {
                                EmprendeChatBubble(
                                    mensaje = msg,
                                    esMio = esMio,
                                    palette = palette,
                                    audioState = audioPlaybackState,
                                    onMantenerPresionado = { mensajeOpcionesActivo = msg },
                                    onAbrirImagen = { url -> imagenPantallaCompletaUrl = url },
                                    onToggleAudio = { msgKey, url, dur ->
                                        audioPlayer.togglePlay(msgKey, url, dur)
                                    },
                                    onAbrirDocumento = { url ->
                                        ChatDeviceHelper.abrirUrlExterna(context, url)
                                    },
                                    onAbrirUbicacion = { lat, lng, label ->
                                        ChatDeviceHelper.abrirMapaUbicacion(context, lat, lng, label)
                                    },
                                    onCopiarContacto = { tel ->
                                        ChatDeviceHelper.copiarAlPortapapeles(context, "Teléfono", tel)
                                    },
                                    onLlamarContacto = { tel ->
                                        ChatDeviceHelper.abrirMarcadorTelefonico(context, tel)
                                    }
                                )
                            }
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

    imagenPantallaCompletaUrl?.let { url ->
        ChatFullScreenImageDialog(
            imageUrl = url,
            onDismiss = { imagenPantallaCompletaUrl = null }
        )
    }

    mensajeOpcionesActivo?.let { msgSeleccionado ->
        val esMioSel = msgSeleccionado.remitenteId == miUsuarioId
        val payloadSel = ChatMessageCodec.decode(msgSeleccionado.contenido)
        val esElimSel = payloadSel.tipo == ChatMediaTipo.DELETED
        val textoCopiable = if (payloadSel.tipo == ChatMediaTipo.TEXT) payloadSel.text else null

        ChatMessageActionsDialog(
            esMio = esMioSel,
            esEliminado = esElimSel,
            esBloqueada = estaBloqueada,
            textoCopiable = textoCopiable,
            palette = palette,
            onResponder = {
                mensajeAResponder = ChatReplyQuote(
                    messageId = msgSeleccionado.id,
                    senderName = if (esMioSel) "Tú" else nombreTitulo,
                    preview = ChatMessageCodec.resumenLegible(msgSeleccionado.contenido)
                )
            },
            onCopiarTexto = {
                if (!textoCopiable.isNullOrBlank()) {
                    ChatDeviceHelper.copiarAlPortapapeles(context, "Mensaje", textoCopiable)
                }
            },
            onEliminarParaMi = { onEliminarMensajeParaMi(msgSeleccionado) },
            onEliminarParaTodos = { onEliminarMensajeParaTodos(msgSeleccionado) },
            onDismiss = { mensajeOpcionesActivo = null }
        )
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
                Text("Se suspenderán los mensajes y los pedidos entre ambas partes hasta que desbloquees la conversación.")
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
