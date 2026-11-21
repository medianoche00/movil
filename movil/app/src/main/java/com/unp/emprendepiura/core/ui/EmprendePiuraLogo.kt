package com.unp.emprendepiura.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Archivo: EmprendePiuraLogo.kt
 * Proposito: Identidad visual vectorial exclusiva de EmprendePiura (bolsa marketplace,
 *            sol piurano dorado y trazo ascendente de emprendimiento local).
 */
@Composable
fun EmprendePiuraLogoGlyph(
    modifier: Modifier = Modifier,
    bagColor: Color = Color.White,
    sunColor: Color = Color(0xFFF59E0B),
    sparkColor: Color = Color(0xFF1D4ED8)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun x(v: Float) = (v / 108f) * w
        fun y(v: Float) = (v / 108f) * h
        val unit = size.minDimension / 108f

        // Asa superior de la bolsa marketplace
        val handlePath = Path().apply {
            moveTo(x(40f), y(41f))
            lineTo(x(40f), y(33f))
            cubicTo(x(40f), y(24.5f), x(46.3f), y(18.5f), x(54f), y(18.5f))
            cubicTo(x(61.7f), y(18.5f), x(68f), y(24.5f), x(68f), y(33f))
            lineTo(x(68f), y(41f))
        }
        drawPath(
            path = handlePath,
            color = bagColor,
            style = Stroke(width = 6.2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Cuerpo estilizado de tienda / bolsa
        val bagPath = Path().apply {
            moveTo(x(26.5f), y(39.5f))
            lineTo(x(81.5f), y(39.5f))
            cubicTo(x(85.2f), y(39.5f), x(87.8f), y(42f), x(88.3f), y(45.5f))
            lineTo(x(91.5f), y(78.5f))
            cubicTo(x(92.2f), y(85.2f), x(87.2f), y(90.5f), x(80.5f), y(90.5f))
            lineTo(x(27.5f), y(90.5f))
            cubicTo(x(20.8f), y(90.5f), x(15.8f), y(85.2f), x(16.5f), y(78.5f))
            lineTo(x(19.7f), y(45.5f))
            cubicTo(x(20.2f), y(42f), x(22.8f), y(39.5f), x(26.5f), y(39.5f))
            close()
        }
        drawPath(path = bagPath, color = bagColor)

        // Sol dorado caracteristico de Piura
        drawCircle(
            color = sunColor,
            radius = 8.2f * unit,
            center = Offset(x(40.5f), y(54.5f))
        )

        // Trazo ascendente de crecimiento emprendedor
        val sparkPath = Path().apply {
            moveTo(x(31f), y(77f))
            lineTo(x(46f), y(64.5f))
            lineTo(x(54.5f), y(71f))
            lineTo(x(75f), y(51.5f))
        }
        drawPath(
            path = sparkPath,
            color = sparkColor,
            style = Stroke(width = 6.4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Punta de flecha hacia arriba
        val arrowHead = Path().apply {
            moveTo(x(64f), y(51.2f))
            lineTo(x(75.5f), y(51.2f))
            lineTo(x(75.5f), y(62.5f))
        }
        drawPath(
            path = arrowHead,
            color = sparkColor,
            style = Stroke(width = 6.4f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun EmprendePiuraBrandBadge(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    cornerRadius: Dp = 20.dp,
    elevatedShadow: Boolean = true
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        modifier = modifier
            .size(size)
            .then(
                if (elevatedShadow) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        spotColor = Color(0xFF1D4ED8).copy(alpha = 0.45f)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF3B82F6),
                            0.5f to Color(0xFF1D4ED8),
                            1.0f to Color(0xFF0A2E8A)
                        )
                    )
                )
                .padding(size * 0.14f),
            contentAlignment = Alignment.Center
        ) {
            EmprendePiuraLogoGlyph(modifier = Modifier.fillMaxSize())
        }
    }
}
