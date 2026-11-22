package com.unp.emprendepiura.features.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.domain.model.Mensaje

/*
 * Archivo: ChatComponents.kt
 * Proposito: Componentes visuales reutilizables de mensajeria propios de EmprendePiura:
 *            burbujas con doble visto azul reactivo, indicador de escritura en vivo y
 *            separadores de fecha por dia.
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
fun EmprendeChatBubble(
    mensaje: Mensaje,
    esMio: Boolean,
    palette: EmprendeChatPalette,
    modifier: Modifier = Modifier
) {
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

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = if (esMio) Color.Transparent else palette.incomingBubble,
            border = BorderStroke(
                width = 1.dp,
                color = if (esMio) palette.outgoingBubbleBorder else palette.incomingBubbleBorder
            ),
            shadowElevation = if (palette.isDark) 0.dp else 1.2.dp,
            modifier = Modifier.widthIn(min = 78.dp, max = 296.dp)
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (esMio) {
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(palette.outgoingBubbleStart, palette.outgoingBubbleEnd)
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(start = 13.dp, end = 11.dp, top = 8.dp, bottom = 7.dp)
            ) {
                Text(
                    text = mensaje.contenido,
                    fontSize = 14.5.sp,
                    lineHeight = 19.5.sp,
                    color = if (esMio) palette.outgoingText else palette.incomingText
                )

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = hora,
                        fontSize = 10.5.sp,
                        color = if (esMio) palette.outgoingMeta else palette.incomingMeta
                    )
                    if (esMio) {
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
