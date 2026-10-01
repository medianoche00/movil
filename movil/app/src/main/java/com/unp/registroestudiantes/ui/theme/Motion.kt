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

/*
 * Archivo: Motion.kt
 * Proposito: Definicion de fisicas de animacion y modificadores tactiles estilo Apple (iOS).
 *
 * Guia de transicion CSS / JavaScript -> Jetpack Compose:
 *
 * 1. Animaciones basadas en fisica (Spring Physics) vs Curvas Beziér:
 *    Las animaciones tradicionales usan funciones de tiempo rigidas (ease-in, ease-out).
 *    En sistemas modernos como iOS, las animaciones usan modelos fisicos de resorte (Spring):
 *    - DampingRatio (amortiguamiento): Controla que tanto rebota el elemento al detenerse.
 *      MediumBouncy produce un rebote tactil agradable.
 *    - Stiffness (rigidez): Controla la velocidad con la que el resorte busca el equilibrio.
 *
 * 2. Modificador personalizado con funcion de extension:
 *    'fun Modifier.pressScale(): Modifier'
 *    Agrega una propiedad a la cadena de modificadores de cualquier componente grafico
 *    (botones, tarjetas, imagenes).
 *    En CSS equivaldria a:
 *      button:active {
 *          transform: scale(0.97);
 *          transition: transform 0.2s cubic-bezier(...);
 *      }
 *
 * 3. Delegado de propiedad 'by' y 'remember':
 *    - remember { ... }: Mantiene la instancia viva durante las recomposiciones de Compose.
 *    - by: Delegado de Kotlin que extrae automaticamente el valor '.value' de un State,
 *      simplificando la sintaxis para que 'pressed' se lea directamente como Boolean.
 */

/**
 * Especificacion de resorte con rebote natural estilo Apple.
 * Es una funcion generica (<T>) para poder aplicarse a valores Float, Dp, Color, etc.
 */
fun <T> appleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

/**
 * Modificador reutilizable que reduce sutilmente la escala del elemento al 97%
 * cuando el usuario mantiene presionado el dedo sobre el, regresando con un rebote
 * suave al soltarlo.
 */
@Composable
fun Modifier.pressScale(): Modifier {
    // Fuente de eventos de interaccion (toques, arrastres, foco)
    val interaction = remember { MutableInteractionSource() }

    // Estado booleano reactivo que se activa cuando el usuario toca la pantalla
    val pressed by interaction.collectIsPressedAsState()

    // Anima el valor de escala entre 0.97f y 1.0f usando la fisica de resorte
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = appleSpring(),
        label = "pressScale"
    ).value

    // Aplica la transformacion de escala al Modifier
    return this.scale(scale)
}
