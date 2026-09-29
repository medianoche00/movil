package com.unp.registroestudiantes.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/** Spring "estilo Apple": rebote medio, sin sentirse lineal. */
fun <T> appleSpring() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

/** Aplica un scale(0.97) suave mientras el elemento está presionado. */
@Composable
fun Modifier.pressScale(): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f, animationSpec = appleSpring(), label = "pressScale"
    ).value
    return this.scale(scale)
}
