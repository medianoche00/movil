package com.unp.emprendepiura.features.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction

/*
 * Archivo: ChatAttachmentUi.kt
 * Proposito: Componentes modulares para la bandeja de adjuntos (Galeria, Camara, Ubicacion,
 *            Contacto y Documento), barra de respuesta citada, grabador de audio en vivo
 *            y visor de fotos en pantalla completa.
 */
@Composable
fun ChatAttachmentTray(
    visible: Boolean,
    palette: EmprendeChatPalette,
    onGaleria: () -> Unit,
    onCamara: () -> Unit,
    onUbicacion: () -> Unit,
    onContacto: () -> Unit,
    onDocumento: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(240, easing = AppleNavigationEasing)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(200, easing = AppleNavigationEasing)) + fadeOut(tween(140)),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            color = palette.barSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, palette.barDivider),
            shadowElevation = if (palette.isDark) 0.dp else 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.incomingMeta.copy(alpha = 0.35f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentPillItem(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Galería",
                        tint = Color(0xFF2563EB),
                        palette = palette,
                        onClick = onGaleria,
                        modifier = Modifier.weight(1f)
                    )
                    AttachmentPillItem(
                        icon = Icons.Default.PhotoCamera,
                        label = "Cámara",
                        tint = Color(0xFFE11D48),
                        palette = palette,
                        onClick = onCamara,
                        modifier = Modifier.weight(1f)
                    )
                    AttachmentPillItem(
                        icon = Icons.Default.LocationOn,
                        label = "Ubicación",
                        tint = Color(0xFF059669),
                        palette = palette,
                        onClick = onUbicacion,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentPillItem(
                        icon = Icons.Default.Person,
                        label = "Contacto",
                        tint = Color(0xFF0284C7),
                        palette = palette,
                        onClick = onContacto,
                        modifier = Modifier.weight(1f)
                    )
                    AttachmentPillItem(
                        icon = Icons.Default.Description,
                        label = "Documento",
                        tint = Color(0xFF7C3AED),
                        palette = palette,
                        onClick = onDocumento,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AttachmentPillItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    palette: EmprendeChatPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val press = rememberPressInteraction()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = press, indication = null, onClick = onClick)
            .pressScale(press)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = tint.copy(alpha = if (palette.isDark) 0.18f else 0.10f),
            border = BorderStroke(1.dp, tint.copy(alpha = 0.25f)),
            modifier = Modifier
                .width(72.dp)
                .height(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChatReplyPreviewBar(
    quote: ChatReplyQuote?,
    palette: EmprendeChatPalette,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = quote != null,
        enter = expandVertically(tween(200, easing = AppleNavigationEasing)) + fadeIn(tween(160)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
        modifier = modifier.fillMaxWidth()
    ) {
        quote?.let { q ->
            Surface(
                color = palette.inputSurface,
                border = BorderStroke(1.dp, palette.barDivider),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(palette.accentPrimary)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = q.senderName.ifBlank { "Mensaje" },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = q.preview,
                            fontSize = 12.sp,
                            color = palette.incomingMeta,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar respuesta",
                            tint = palette.incomingMeta,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatAudioRecordingBar(
    duracionSeg: Int,
    niveles: List<Float>,
    palette: EmprendeChatPalette,
    onCancelar: () -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "recPulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recDotAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val cancelPress = rememberPressInteraction()
        FilledTonalIconButton(
            onClick = onCancelar,
            interactionSource = cancelPress,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .size(46.dp)
                .pressScale(cancelPress)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Cancelar nota de voz", modifier = Modifier.size(20.dp))
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.inputSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(dotAlpha)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                )
                Text(
                    text = ChatMessageCodec.formatearDuracion(duracionSeg),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End)
                ) {
                    niveles.takeLast(18).forEach { nivel ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight(nivel.coerceIn(0.18f, 1f))
                                .clip(RoundedCornerShape(50))
                                .background(palette.accentPrimary)
                        )
                    }
                }
            }
        }

        val sendPress = rememberPressInteraction()
        FilledIconButton(
            onClick = onEnviar,
            interactionSource = sendPress,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = palette.accentPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .size(48.dp)
                .pressScale(sendPress)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar nota de voz", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ChatFullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Imagen del chat",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
            FilledTonalIconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar imagen")
            }
        }
    }
}

@Composable
fun ChatMessageActionsDialog(
    esMio: Boolean,
    esEliminado: Boolean,
    esBloqueada: Boolean,
    textoCopiable: String?,
    palette: EmprendeChatPalette,
    onResponder: () -> Unit,
    onCopiarTexto: () -> Unit,
    onEliminarParaMi: () -> Unit,
    onEliminarParaTodos: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Opciones del mensaje",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!esBloqueada && !esEliminado) {
                    MessageActionRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        label = "Responder",
                        tint = palette.accentPrimary,
                        onClick = {
                            onDismiss()
                            onResponder()
                        }
                    )
                }

                if (!esEliminado && !textoCopiable.isNullOrBlank()) {
                    MessageActionRow(
                        icon = Icons.Default.ContentCopy,
                        label = "Copiar texto",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            onDismiss()
                            onCopiarTexto()
                        }
                    )
                }

                HorizontalDivider(color = palette.barDivider)

                MessageActionRow(
                    icon = Icons.Default.Delete,
                    label = "Eliminar para mí",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        onEliminarParaMi()
                    }
                )

                if (esMio && !esEliminado) {
                    MessageActionRow(
                        icon = Icons.Default.DeleteForever,
                        label = "Eliminar para todos",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDismiss()
                            onEliminarParaTodos()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MessageActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = tint)
        }
    }
}
