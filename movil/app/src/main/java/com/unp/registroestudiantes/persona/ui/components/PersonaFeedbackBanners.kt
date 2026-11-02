package com.unp.registroestudiantes.persona.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unp.registroestudiantes.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Archivo: PersonaFeedbackBanners.kt
 * Proposito: Banner de notificacion flotante superior con auto-cierre temporizado (4.5s),
 *            boton 'X' y cierre al deslizar hacia arriba o hacia los costados.
 */
@Composable
fun PersonaFeedbackBanners(
    errorMessage: String?,
    successMessage: String?,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragX by remember(errorMessage, successMessage) { mutableFloatStateOf(0f) }
    var dragY by remember(errorMessage, successMessage) { mutableFloatStateOf(0f) }

    val animX by animateFloatAsState(
        targetValue = dragX,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "personaBannerX"
    )
    val animY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "personaBannerY"
    )

    // Auto-cierre tras 4.5 segundos
    LaunchedEffect(errorMessage, successMessage) {
        if (errorMessage != null || successMessage != null) {
            delay(4500)
            onClearMessages()
        }
    }

    val visible = errorMessage != null || successMessage != null
    val esError = errorMessage != null
    val mensaje = errorMessage ?: successMessage ?: ""
    val fadeProgress = (1f - (abs(animX) / 320f) - (abs(animY.coerceAtMost(0f)) / 180f)).coerceIn(0.25f, 1f)

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .zIndex(100f),
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { -it } + fadeIn(tween(180)),
        exit = slideOutVertically(
            animationSpec = tween(220)
        ) { -it } + fadeOut(tween(200))
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (esError) Color(0xFFD32F2F) else Color(0xFF2E7D32),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animX.roundToInt(), animY.roundToInt()) }
                .alpha(fadeProgress)
                .pointerInput(errorMessage, successMessage) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragY < -40f || abs(dragX) > 75f) {
                                onClearMessages()
                            } else {
                                dragX = 0f
                                dragY = 0f
                            }
                        },
                        onDragCancel = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX += dragAmount.x
                            dragY = (dragY + dragAmount.y).coerceAtMost(18f)
                        }
                    )
                }
                .pressScale()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (esError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (esError) "Aviso del Sistema" else "Operación Exitosa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = mensaje,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onClearMessages,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
