package com.unp.registroestudiantes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Archivo: Theme.kt
 * Proposito: Sistema de diseno centralizado (colores, tipografia y formas) usando Material 3.
 *
 * Guia de transicion Java/XML y CSS -> Jetpack Compose:
 *
 * 1. Temas en Compose vs res/values/styles.xml:
 *    En Android clasico con XML, los estilos se definian en styles.xml con etiquetas
 *    <style name="..."><item name="colorPrimary">...</item></style>.
 *    En Compose, todo el diseno se define en codigo Kotlin puro y con seguridad de tipos.
 *    Equivale a las variables CSS (:root { --color-primary: #0A84FF; }) en desarrollo web.
 *
 * 2. Notacion de colores hexadecimales (0xFF...):
 *    'Color(0xFF0A84FF)': Los dos primeros caracteres 'FF' corresponden al canal alfa
 *    (opacidad completa = 255). Los siguientes 6 caracteres '0A84FF' son el RGB clasico.
 *
 * 3. Unidades de medida (dp y sp):
 *    - dp (density-independent pixels): Para tamanos de vistas, margenes y paddings.
 *      Garantiza que un elemento mida fisicamente lo mismo en un celular pequeno o grande.
 *    - sp (scale-independent pixels): Exclusivo para texto. Respeta la configuracion
 *      de accesibilidad del usuario (si el usuario agranda la letra en ajustes del sistema).
 *
 * 4. Shapes (Bordes redondeados globales):
 *    En XML habia que crear decenas de archivos res/drawable/button_rounded.xml.
 *    En Compose, 'Shapes' define el radio de curvatura para componentes pequenos,
 *    medianos y grandes de toda la aplicacion en un unico lugar.
 */

// ============================================================
// 1. Paleta de Colores
// ============================================================

private val Blue = Color(0xFF0A84FF)       // Azul primario estilo iOS/Apple
private val BlueDark = Color(0xFF64B5FF)   // Azul adaptado para modo oscuro
private val Bg = Color(0xFFF7F7FA)         // Fondo claro neutral
private val Surface = Color(0xFFFFFFFF)    // Superficie blanca para tarjetas

// Esquema de colores para Modo Claro
private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    background = Bg,
    surface = Surface,
    secondaryContainer = Color(0xFFE8F0FE)
)

// Esquema de colores para Modo Oscuro
private val DarkColors = darkColorScheme(
    primary = BlueDark,
    onPrimary = Color.Black,
    background = Color(0xFF0E0E10),
    surface = Color(0xFF1C1C1E)
)

// ============================================================
// 2. Formas y Bordes (Shapes)
// ============================================================

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

// ============================================================
// 3. Tipografia Global (Typography)
// ============================================================

val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

// ============================================================
// 4. Tema Principal Composable
// ============================================================

/**
 * Funcion envoltorio que inyecta la configuracion visual a todos sus componentes hijos.
 * El parametro 'content: @Composable () -> Unit' equivale al 'children' en React
 * o a un slot en Vue/Web Components.
 */
@Composable
fun AppTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
