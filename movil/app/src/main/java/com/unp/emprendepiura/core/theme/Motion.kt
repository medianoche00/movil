package com.unp.emprendepiura.core.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/*
 * Archivo: Motion.kt
 * Proposito: Fisicas y transiciones estilo Apple (iOS spring + curvas cubic-bezier con parallax)
 *            reutilizadas en navegacion, tarjetas tactiles y cambio de pestanas.
 */
val AppleNavigationEasing = CubicBezierEasing(0.28f, 0.11f, 0.25f, 1.0f)

fun <T> softSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

fun <T> appleBouncySpring() = spring<T>(
    dampingRatio = 0.72f,
    stiffness = 460f
)

fun <T> applePushSpring() = spring<T>(
    dampingRatio = 0.88f,
    stiffness = 420f
)

fun appleIntOffsetTween(durationMillis: Int = 380) =
    tween<IntOffset>(durationMillis = durationMillis, easing = AppleNavigationEasing)

@Composable
fun rememberPressInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }

fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = appleBouncySpring(),
        label = "applePressScale"
    )
    this.scale(scale)
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.applePushEnter(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = appleIntOffsetTween(390)
    ) + fadeIn(animationSpec = tween(210))

fun AnimatedContentTransitionScope<NavBackStackEntry>.applePushExit(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = appleIntOffsetTween(390),
        targetOffset = { (it * 0.28f).toInt() }
    ) + fadeOut(animationSpec = tween(360, easing = AppleNavigationEasing)) +
        scaleOut(targetScale = 0.96f, animationSpec = tween(360, easing = AppleNavigationEasing))

fun AnimatedContentTransitionScope<NavBackStackEntry>.applePopEnter(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = appleIntOffsetTween(370),
        initialOffset = { (it * 0.28f).toInt() }
    ) + fadeIn(animationSpec = tween(320, easing = AppleNavigationEasing)) +
        scaleIn(initialScale = 0.96f, animationSpec = tween(350, easing = AppleNavigationEasing))

fun AnimatedContentTransitionScope<NavBackStackEntry>.applePopExit(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = appleIntOffsetTween(370)
    ) + fadeOut(animationSpec = tween(250, delayMillis = 60))

fun appleSubRouteTransition(esRegresoATabs: Boolean): ContentTransform {
    return if (!esRegresoATabs) {
        (slideInHorizontally(appleIntOffsetTween(380)) { it } +
            fadeIn(tween(220))) togetherWith
            (slideOutHorizontally(appleIntOffsetTween(380)) { -(it * 0.28f).toInt() } +
                fadeOut(tween(340, easing = AppleNavigationEasing)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(340)))
    } else {
        (slideInHorizontally(appleIntOffsetTween(360)) { -(it * 0.28f).toInt() } +
            fadeIn(tween(300, easing = AppleNavigationEasing)) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(320))) togetherWith
            (slideOutHorizontally(appleIntOffsetTween(360)) { it } +
                fadeOut(tween(240, delayMillis = 50)))
    }
}

fun appleTabTransition(): ContentTransform =
    (fadeIn(tween(230, easing = AppleNavigationEasing)) +
        scaleIn(initialScale = 0.985f, animationSpec = tween(250, easing = AppleNavigationEasing))) togetherWith
        fadeOut(tween(150))
