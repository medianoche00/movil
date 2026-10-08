package com.unp.registroestudiantes.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Archivo: Components.kt
 * Proposito: Componentes visuales genericos y reutilizables en cualquier pantalla.
 *
 * Guia de transicion CSS -> Jetpack Compose:
 *
 * 1. Efecto Shimmer (Skeleton loading placeholder):
 *    Es el patron visual comun en apps como Facebook, LinkedIn o YouTube donde
 *    se muestra un bloque gris con un reflejo de luz que se desplaza horizontalmente
 *    mientras el contenido real termina de descargarse de internet.
 *
 * 2. rememberInfiniteTransition:
 *    Equivale a una animacion CSS infinita:
 *      @keyframes shimmer { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }
 *      .placeholder { animation: shimmer 1.1s linear infinite; }
 *
 * 3. Brush.linearGradient:
 *    Equivalente directo a 'background: linear-gradient(...)' en CSS.
 *    Al mover dinamicamente los puntos Offset(x, 0f) y Offset(x + 200f, 0f),
 *    la franja de luz se desplaza continuamente a lo largo de la caja.
 */

/**
 * Bloque rectangular con efecto de brillo desplazante (shimmer) para estados de carga.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    // Crea una transicion que se repite de manera infinita
    val transition = rememberInfiniteTransition(label = "shimmer")

    // Anima la coordenada X de la luz desde -400px hasta 400px en un bucle continuo de 1100ms
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    // Colores del gradiente obtenidos del tema actual
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(x, 0f),
                    end = Offset(x + 200f, 0f)
                )
            )
    )
}
