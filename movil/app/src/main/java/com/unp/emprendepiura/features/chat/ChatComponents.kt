package com.unp.emprendepiura.features.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.domain.model.Mensaje
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Archivo: ChatComponents.kt
 * Proposito: Burbujas enriquecidas de EmprendePiura (texto, foto, audio, documento,
 *            ubicacion, contacto y respuestas citadas), gesto deslizar para responder
 *            en ambas direcciones, doble visto azul reactivo e indicador de escritura.
 */
@Composable
fun TypingIndicatorDots(
    modifier: Modifier = Modifier,
    puntoColor: Color,
    tamanoPunto: Float = 6.5f
) {
    val transition = rememberInfiniteTransition(label = "emprendeTypingDots")

    val offset1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val offset2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val offset3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, delayMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(offset1, offset2, offset3).forEach { yOffset ->
            Box(
                modifier = Modifier
                    .offset(y = yOffset.dp)
                    .size(tamanoPunto.dp)
                    .clip(CircleShape)
                    .background(puntoColor)
            )
        }
    }
}

@Composable
fun TypingBubbleMessage(
    nombreContraparte: String,
    palette: EmprendeChatPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 5.dp),
            color = palette.incomingBubble,
            border = BorderStroke(1.dp, palette.incomingBubbleBorder),
            shadowElevation = if (palette.isDark) 0.dp else 1.dp,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "$nombreContraparte está escribiendo",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.incomingMeta
                )
                TypingIndicatorDots(puntoColor = palette.accentPrimary, tamanoPunto = 5.5f)
            }
        }
    }
}

@Composable
fun SwipeableReplyMessageContainer(
    enabled: Boolean,
    palette: EmprendeChatPalette,
    onResponder: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 56.dp.toPx() }
    val maxOffsetPx = with(density) { 74.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var hapticsDisparado by remember { mutableStateOf(false) }

    val progreso = (abs(offsetX.value) / thresholdPx).coerceIn(0f, 1f)
    val haciaDerecha = offsetX.value >= 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { hapticsDisparado = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val nuevo = (offsetX.value + dragAmount * 0.72f).coerceIn(-maxOffsetPx, maxOffsetPx)
                        scope.launch { offsetX.snapTo(nuevo) }
                        if (!hapticsDisparado && abs(nuevo) >= thresholdPx) {
                            hapticsDisparado = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDragEnd = {
                        val disparo = abs(offsetX.value) >= thresholdPx
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = applePushSpring())
                        }
                        if (disparo) onResponder()
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = applePushSpring())
                        }
                    }
                )
            }
    ) {
        if (progreso > 0.08f) {
            Box(
                modifier = Modifier
                    .align(if (haciaDerecha) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 8.dp)
                    .size(34.dp)
                    .scale(0.65f + 0.35f * progreso)
                    .alpha(progreso)
                    .clip(CircleShape)
                    .background(palette.accentPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Responder",
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmprendeChatBubble(
    mensaje: Mensaje,
    esMio: Boolean,
    palette: EmprendeChatPalette,
    audioState: AudioPlaybackState = AudioPlaybackState(),
    onMantenerPresionado: () -> Unit = {},
    onAbrirImagen: (String) -> Unit = {},
    onToggleAudio: (messageId: String, url: String, durationSec: Int) -> Unit = { _, _, _ -> },
    onAbrirDocumento: (String) -> Unit = {},
    onAbrirUbicacion: (lat: Double, lng: Double, label: String) -> Unit = { _, _, _ -> },
    onCopiarContacto: (String) -> Unit = {},
    onLlamarContacto: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val payload = remember(mensaje.contenido) { ChatMessageCodec.decode(mensaje.contenido) }
    val esEliminado = payload.tipo == ChatMediaTipo.DELETED
    val bubbleShape = if (esMio) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
    }

    val hora = formatearHoraLocalChat(mensaje.creadoEn).ifBlank { "Ahora" }

    val checkColor by animateColorAsState(
        targetValue = if (mensaje.leido) palette.checkLeidoBlue else palette.checkEnviado,
        animationSpec = tween(280),
        label = "readCheckColor"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (mensaje.leido) 1.08f else 1f,
        animationSpec = appleBouncySpring(),
        label = "readCheckScale"
    )

    val textColor = if (esMio && !esEliminado) palette.outgoingText else palette.incomingText
    val metaColor = if (esMio && !esEliminado) palette.outgoingMeta else palette.incomingMeta

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = if (esMio && !esEliminado) Color.Transparent else palette.incomingBubble,
            border = BorderStroke(
                width = 1.dp,
                color = if (esMio && !esEliminado) palette.outgoingBubbleBorder else palette.incomingBubbleBorder
            ),
            shadowElevation = if (palette.isDark || esEliminado) 0.dp else 1.2.dp,
            modifier = Modifier
                .widthIn(min = 84.dp, max = 304.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onMantenerPresionado
                )
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (esMio && !esEliminado) {
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(palette.outgoingBubbleStart, palette.outgoingBubbleEnd)
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(start = 11.dp, end = 11.dp, top = 8.dp, bottom = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!esEliminado) {
                    payload.reply?.let { quote ->
                        QuotedReplyStrip(quote = quote, esMio = esMio, palette = palette)
                    }
                }

                when (payload.tipo) {
                    ChatMediaTipo.DELETED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = palette.incomingMeta,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (esMio) "Eliminaste este mensaje" else "Se eliminó este mensaje",
                                fontSize = 13.5.sp,
                                fontStyle = FontStyle.Italic,
                                color = palette.incomingMeta
                            )
                        }
                    }

                    ChatMediaTipo.TEXT -> {
                        Text(
                            text = payload.text,
                            fontSize = 14.5.sp,
                            lineHeight = 19.5.sp,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    ChatMediaTipo.IMAGE -> {
                        if (payload.url.isNotBlank()) {
                            AsyncImage(
                                model = payload.url,
                                contentDescription = "Imagen compartida",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp, max = 230.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .clickable { onAbrirImagen(payload.url) }
                            )
                        }
                        if (payload.text.isNotBlank()) {
                            Text(
                                text = payload.text,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                color = textColor
                            )
                        }
                    }

                    ChatMediaTipo.AUDIO -> {
                        val msgKey = mensaje.id.ifBlank { payload.url }
                        val esEsteAudio = audioState.activeMessageId == msgKey
                        val reproduciendo = esEsteAudio && audioState.isPlaying
                        val preparando = esEsteAudio && audioState.isPreparing
                        val duracionTotalMs = if (esEsteAudio && audioState.durationMs > 0) {
                            audioState.durationMs
                        } else {
                            (payload.durationSec.coerceAtLeast(1)) * 1000
                        }
                        val progresoAudio = if (esEsteAudio && duracionTotalMs > 0) {
                            (audioState.positionMs.toFloat() / duracionTotalMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        val tiempoTexto = if (esEsteAudio && (reproduciendo || audioState.positionMs > 0)) {
                            ChatMessageCodec.formatearDuracion(audioState.positionMs / 1000)
                        } else {
                            ChatMessageCodec.formatearDuracion(payload.durationSec)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .widthIn(min = 215.dp)
                                .padding(vertical = 2.dp)
                        ) {
                            Surface(
                                onClick = { onToggleAudio(msgKey, payload.url, payload.durationSec) },
                                shape = CircleShape,
                                color = if (esMio) Color.White.copy(alpha = 0.22f) else palette.accentPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (preparando) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = if (esMio) Color.White else palette.accentPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (reproduciendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (reproduciendo) "Pausar" else "Reproducir",
                                            tint = if (esMio) Color.White else palette.accentPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                                ) {
                                    val alturas = remember {
                                        listOf(0.35f, 0.7f, 0.45f, 0.9f, 0.6f, 0.35f, 0.85f, 0.55f, 1f, 0.65f, 0.4f, 0.8f, 0.5f, 0.75f, 0.45f, 0.6f)
                                    }
                                    alturas.forEachIndexed { i, h ->
                                        val barraRatio = (i + 1).toFloat() / alturas.size.toFloat()
                                        val activa = barraRatio <= progresoAudio
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(h)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    when {
                                                        esMio && activa -> Color.White
                                                        esMio -> Color.White.copy(alpha = 0.38f)
                                                        activa -> palette.accentPrimary
                                                        else -> palette.incomingMeta.copy(alpha = 0.32f)
                                                    }
                                                )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = metaColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Nota de voz · $tiempoTexto",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = metaColor
                                    )
                                }
                            }
                        }
                    }

                    ChatMediaTipo.DOCUMENT -> {
                        val ext = payload.fileName.substringAfterLast('.', "").uppercase(Locale.US)
                        val colorBadge = when (ext) {
                            "PDF" -> Color(0xFFDC2626)
                            "DOC", "DOCX" -> Color(0xFF2563EB)
                            "XLS", "XLSX", "CSV" -> Color(0xFF15803D)
                            "PPT", "PPTX" -> Color(0xFFD97706)
                            else -> Color(0xFF7C3AED)
                        }
                        Surface(
                            onClick = { onAbrirDocumento(payload.url) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (esMio) Color.White.copy(alpha = 0.16f) else palette.inputSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(colorBadge.copy(alpha = if (esMio) 0.90f else 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (esMio) Color.White else colorBadge,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = payload.fileName.ifBlank { "Documento adjunto" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = ChatMessageCodec.formatearTamanoArchivo(payload.fileSizeBytes)
                                    val info = listOf(ext.ifBlank { "ARCHIVO" }, sizeLabel).filter { it.isNotBlank() }.joinToString(" · ")
                                    Text(text = info, fontSize = 11.sp, color = metaColor)
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Abrir documento",
                                    tint = metaColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    ChatMediaTipo.LOCATION -> {
                        val lat = payload.lat ?: -5.178288
                        val lng = payload.lng ?: -80.654888
                        val label = payload.addressLabel.ifBlank { "Ubicación compartida en Piura" }
                        Surface(
                            onClick = { onAbrirUbicacion(lat, lng, label) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (esMio) Color.White.copy(alpha = 0.16f) else palette.inputSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color(0xFF059669).copy(alpha = if (esMio) 0.90f else 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (esMio) Color.White else Color(0xFF059669),
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Ubicación compartida",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = metaColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = String.format(Locale.US, "GPS: %.5f, %.5f · Toca para ver en mapa", lat, lng),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (esMio) Color.White.copy(alpha = 0.9f) else Color(0xFF059669)
                                )
                            }
                        }
                    }

                    ChatMediaTipo.CONTACT -> {
                        val nombreC = payload.contactName.ifBlank { "Contacto" }
                        val telC = payload.contactPhone.ifBlank { "Sin teléfono" }
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = if (esMio) Color.White.copy(alpha = 0.16f) else palette.inputSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0284C7).copy(alpha = if (esMio) 0.90f else 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (esMio) Color.White else Color(0xFF0284C7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(nombreC, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = textColor)
                                        Text(telC, fontSize = 12.sp, color = metaColor)
                                    }
                                }
                                if (payload.contactPhone.isNotBlank() && !payload.contactPhone.contains("Sin")) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            onClick = { onCopiarContacto(payload.contactPhone) },
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (esMio) Color.White.copy(alpha = 0.2f) else palette.accentPrimary.copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    null,
                                                    tint = if (esMio) Color.White else palette.accentPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Copiar",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (esMio) Color.White else palette.accentPrimary
                                                )
                                            }
                                        }
                                        Surface(
                                            onClick = { onLlamarContacto(payload.contactPhone) },
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (esMio) Color.White.copy(alpha = 0.2f) else Color(0xFF15803D).copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Phone,
                                                    null,
                                                    tint = if (esMio) Color.White else Color(0xFF15803D),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Llamar",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (esMio) Color.White else Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = hora,
                        fontSize = 10.5.sp,
                        color = metaColor
                    )
                    if (esMio && !esEliminado) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = if (mensaje.leido) "Leído" else "Entregado",
                            tint = checkColor,
                            modifier = Modifier
                                .size(15.dp)
                                .scale(checkScale)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotedReplyStrip(
    quote: ChatReplyQuote,
    esMio: Boolean,
    palette: EmprendeChatPalette
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (esMio) Color.White.copy(alpha = 0.16f) else palette.inputSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(if (esMio) Color.White.copy(alpha = 0.85f) else palette.accentPrimary)
            )
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = quote.senderName.ifBlank { "Mensaje" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (esMio) Color.White else palette.accentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = quote.preview,
                    fontSize = 11.5.sp,
                    color = if (esMio) Color.White.copy(alpha = 0.86f) else palette.incomingMeta,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChatDateBadge(
    texto: String,
    palette: EmprendeChatPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.dateBadgeBg,
            border = BorderStroke(1.dp, palette.barDivider.copy(alpha = 0.65f))
        ) {
            Text(
                text = texto,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.dateBadgeText,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 4.dp)
            )
        }
    }
}
