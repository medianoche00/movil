package com.unp.emprendepiura.features.splash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.ui.EmprendePiuraBrandBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    BackHandler(enabled = true) {}

    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val ring1Scale = remember { Animatable(0.75f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Scale = remember { Animatable(0.75f) }
    val ring2Alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }
    val pillAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }
    val screenScale = remember { Animatable(1f) }

    val shimmerTransition = rememberInfiniteTransition(label = "splashShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing)
        ),
        label = "barProgress"
    )

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch { logoAlpha.animateTo(1f, tween(380, easing = AppleNavigationEasing)) }

        launch {
            ring1Alpha.animateTo(0.42f, tween(220))
            ring1Scale.animateTo(1.75f, tween(920, easing = FastOutSlowInEasing))
            ring1Alpha.animateTo(0f, tween(450))
        }
        launch {
            delay(180)
            ring2Alpha.animateTo(0.28f, tween(220))
            ring2Scale.animateTo(2.05f, tween(960, easing = FastOutSlowInEasing))
            ring2Alpha.animateTo(0f, tween(450))
        }

        delay(220)
        launch { textAlpha.animateTo(1f, tween(520, easing = AppleNavigationEasing)) }
        launch {
            textOffsetY.animateTo(
                0f,
                spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        delay(170)
        pillAlpha.animateTo(1f, tween(480, easing = AppleNavigationEasing))

        delay(1150)
        launch { screenScale.animateTo(1.04f, tween(360, easing = AppleNavigationEasing)) }
        screenAlpha.animateTo(0f, tween(340, easing = AppleNavigationEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(screenScale.value)
            .alpha(screenAlpha.value)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF0A2472),
                        0.45f to Color(0xFF1D4ED8),
                        1.00f to Color(0xFF2563EB)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Resplandor solar piurano y circulos ambientales
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.24f),
                        Color(0xFFF59E0B).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.14f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.14f),
                radius = size.width * 0.72f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.045f),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.10f, size.height * 0.88f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.width * 0.36f,
                center = Offset(size.width * 0.86f, size.height * 0.74f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(164.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(ring1Scale.value)
                        .alpha(ring1Alpha.value)
                ) {
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = size.minDimension / 2f - 1.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Canvas(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(ring2Scale.value)
                        .alpha(ring2Alpha.value)
                ) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2f - 1.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                EmprendePiuraBrandBadge(
                    size = 114.dp,
                    cornerRadius = 32.dp,
                    elevatedShadow = true,
                    modifier = Modifier
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )
            }

            Spacer(Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .offset(y = textOffsetY.value.dp)
                    .alpha(textAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EmprendePiura",
                    color = Color.White,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Marketplace de Emprendedores · Piura y UNP",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.alpha(pillAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashHighlightChip(
                    icon = Icons.Default.Storefront,
                    label = "Tiendas Locales",
                    tint = Color(0xFFFBBF24)
                )
                SplashHighlightChip(
                    icon = Icons.Default.LocalMall,
                    label = "Catálogo UNP",
                    tint = Color.White
                )
                SplashHighlightChip(
                    icon = Icons.Default.LocationOn,
                    label = "Piura",
                    tint = Color(0xFFFBBF24)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .alpha(pillAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Indicador de carga suave estilo Apple
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.9f),
                                    Color(0xFFFBBF24).copy(alpha = 0.85f),
                                    Color.Transparent
                                ),
                                startX = shimmerProgress * 220f - 80f,
                                endX = shimmerProgress * 220f + 80f
                            )
                        )
                )
            }

            Text(
                text = "Impulsando el talento piurano · v1.0",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SplashHighlightChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
