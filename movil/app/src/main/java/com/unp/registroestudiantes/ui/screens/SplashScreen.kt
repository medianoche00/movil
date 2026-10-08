package com.unp.registroestudiantes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Archivo: SplashScreen.kt
 * Proposito: Pantalla de presentacion animada con diseno estetico y dinamicas estilo Apple.
 *
 * Guia de transicion HTML5 Canvas / CSS Animations -> Jetpack Compose:
 *
 * 1. Secuencia de animacion con Corrutinas y LaunchedEffect:
 *    'LaunchedEffect(Unit)' ejecuta un bloque de corrutinas una sola vez cuando
 *    la pantalla entra en composicion (equivalente al hook useEffect(..., []) en React).
 *    Al usar 'launch { ... }' simultaneos, se ejecutan animaciones paralelas:
 *      - El birrete universitario escala con rebote elastico (spring).
 *      - El anillo de pulso se expande y desvanece suavemente.
 *      - Los textos de la universidad aparecen escalonados tras un breve retardo.
 *
 * 2. Objeto Animatable:
 *    Representa un valor numerico que puede interpolarse continuamente hacia un objetivo
 *    (animateTo). A diferencia de CSS, donde las transiciones estan ligadas al DOM,
 *    en Compose 'Animatable' almacena el valor exacto en memoria en cada cuadro de dibujo.
 *
 * 3. Dibujo vectorial con Canvas y Path:
 *    Equivale al elemento <canvas> de HTML5 o a dibujar rutas en SVG:
 *    - Path().apply { moveTo(...); lineTo(...); cubicTo(...); close() }
 *    - Dibuja el birrete academico con lineas y curvas de Beziér sin depender de recursos externos.
 *
 * 4. BackHandler(enabled = true):
 *    Intercepta el gesto o boton 'Atras' del sistema Android durante los 2 segundos
 *    que dura la bienvenida, evitando que el usuario cierre la app accidentalmente.
 */

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Deshabilita el boton Atras durante la animacion
    BackHandler(enabled = true) {}

    // Valores animables independientes
    val logoScale   = remember { Animatable(0.50f) }
    val logoAlpha   = remember { Animatable(0f) }
    val ringScale   = remember { Animatable(0.75f) }
    val ringAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    // Orquesta la linea de tiempo de las animaciones
    LaunchedEffect(Unit) {
        // Fase 1: Entrada del logo con rebote fisico (Spring)
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            logoAlpha.animateTo(1f, tween(480))
        }

        // Fase 2: Anillo de pulso expansivo
        launch {
            ringAlpha.animateTo(0.30f, tween(300))
            ringScale.animateTo(1.55f, tween(900, easing = FastOutSlowInEasing))
            ringAlpha.animateTo(0f, tween(500))
        }

        // Fase 3: Aparicion gradual del texto institucional
        delay(320)
        textAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))

        // Fase 4: Pausa de lectura para el usuario
        delay(1550)

        // Fase 5: Disolucion suave (Fade-out) y transicion a la pantalla principal
        screenAlpha.animateTo(0f, tween(480, easing = FastOutLinearInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF1A72FF),
                        0.55f to Color(0xFF0A52D4),
                        1.00f to Color(0xFF003DAA)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Circulos sutiles de fondo para profundidad visual
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.88f, size.height * 0.08f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.12f, size.height * 0.88f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Contenedor del isotipo y anillo de pulso
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(148.dp)
            ) {
                // Anillo exterior animado
                Canvas(
                    modifier = Modifier
                        .size(148.dp)
                        .scale(ringScale.value)
                        .alpha(ringAlpha.value)
                ) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2f - 1.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Tarjeta esmerilada con el birrete
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    GraduationCapCanvas(modifier = Modifier.size(62.dp))
                }
            }

            Spacer(Modifier.height(38.dp))

            // Textos institucionales con animacion escalonada
            Column(
                modifier = Modifier.alpha(textAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Registro",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Estudiantes",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(14.dp))

                // Linea horizontal decorativa
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Universidad Nacional de Piura",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Pie de pantalla discreto
        Text(
            text = "Sistema de Registro Académico · v1.0",
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .alpha(textAlpha.value)
        )
    }
}

/**
 * Dibuja un birrete universitario mediante trazado de vectores (Path) en Compose Canvas.
 */
@Composable
private fun GraduationCapCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val white = Color.White

        // Diamante o rombo superior del birrete
        val capTop = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)  // Vertice superior
            lineTo(w * 0.95f, h * 0.34f)  // Vertice derecho
            lineTo(w * 0.50f, h * 0.58f)  // Vertice inferior
            lineTo(w * 0.05f, h * 0.34f)  // Vertice izquierdo
            close()
        }
        drawPath(capTop, white)

        // Base curva del birrete
        val capBody = Path().apply {
            moveTo(w * 0.22f, h * 0.43f)
            lineTo(w * 0.22f, h * 0.66f)
            cubicTo(
                w * 0.22f, h * 0.84f,
                w * 0.78f, h * 0.84f,
                w * 0.78f, h * 0.66f
            )
            lineTo(w * 0.78f, h * 0.43f)
            lineTo(w * 0.50f, h * 0.58f)
            close()
        }
        drawPath(capBody, white)

        // Cordon de la borla colgante
        drawLine(
            color = white,
            start = Offset(w * 0.95f, h * 0.34f),
            end   = Offset(w * 0.95f, h * 0.65f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )

        // Bola terminal de la borla
        drawCircle(
            color = white,
            radius = w * 0.072f,
            center = Offset(w * 0.95f, h * 0.74f)
        )
    }
}
