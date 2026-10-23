package com.unp.registroestudiantes.persona.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unp.registroestudiantes.ui.theme.pressScale
import kotlinx.coroutines.delay

/*
 * Archivo: PersonaFeedbackBanners.kt
 * Proposito: Banner de notificacion flotante superior con auto-cierre temporizado estilo Apple.
 */
@Composable
fun PersonaFeedbackBanners(
    errorMessage: String?,
    successMessage: String?,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .pressScale()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono circular
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

                // Mensaje informativo
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

                // Boton de cierre manual
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
