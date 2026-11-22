package com.unp.emprendepiura.features.checkout.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.appleBouncySpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.util.CodigoEntregaUtil
import com.unp.emprendepiura.domain.model.Orden
import kotlinx.coroutines.delay

/*
 * Archivo: PinEntregaComponents.kt
 * Proposito: Tarjeta de PIN dinamico de 6 digitos (rotacion cada 90 s) para el comprador
 *            y dialogo de validacion antifraude para el emprendedor.
 */
@Composable
fun DynamicDeliveryPinCard(
    ordenId: String,
    codigoOrden: String,
    onCopiarPin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nowSec by remember { mutableLongStateOf(System.currentTimeMillis() / 1000L) }

    LaunchedEffect(ordenId, codigoOrden) {
        while (true) {
            nowSec = System.currentTimeMillis() / 1000L
            delay(1000L)
        }
    }

    val ventana = CodigoEntregaUtil.ventanaActual(nowSec)
    val segundosRestantes = CodigoEntregaUtil.segundosRestantes(nowSec)
    val pinActual = remember(ordenId, codigoOrden, ventana) {
        CodigoEntregaUtil.obtenerCodigoDinamico(ordenId, codigoOrden, ventana)
    }

    val progresoObjetivo = segundosRestantes.toFloat() / CodigoEntregaUtil.PERIODO_SEGUNDOS.toFloat()
    val progresoAnimado by animateFloatAsState(
        targetValue = progresoObjetivo.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = LinearEasing),
        label = "totpProgress"
    )

    val urgente = segundosRestantes <= 20
    val accentColor by animateColorAsState(
        targetValue = if (urgente) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "totpAccent"
    )

    val tiempoFormato = remember(segundosRestantes) {
        val m = segundosRestantes / 60
        val s = segundosRestantes % 60
        "%02d:%02d".format(m, s)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = accentColor.copy(alpha = 0.09f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopiarPin(pinActual) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Anillo regresivo de vigencia (90 s)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 3.5.dp.toPx()
                                val inset = strokePx / 2f
                                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                                drawArc(
                                    color = accentColor.copy(alpha = 0.18f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = accentColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progresoAnimado,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = tiempoFormato,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = accentColor
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "PIN dinámico · se renueva cada 90 s",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AnimatedContent(
                                targetState = pinActual,
                                transitionSpec = {
                                    (fadeIn(tween(240)) + scaleIn(initialScale = 0.92f, animationSpec = appleBouncySpring())) togetherWith
                                        fadeOut(tween(150))
                                },
                                label = "totpPinText"
                            ) { code ->
                                Text(
                                    text = CodigoEntregaUtil.formatearPin6(code),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 21.sp,
                                    letterSpacing = 1.8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    val copyPress = rememberPressInteraction()
                    FilledTonalIconButton(
                        onClick = { onCopiarPin(pinActual) },
                        interactionSource = copyPress,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = accentColor
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .pressScale(copyPress)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar PIN",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Barra de progreso lineal sutil al pie de la tarjeta PIN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accentColor.copy(alpha = 0.14f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progresoAnimado)
                            .clip(RoundedCornerShape(50))
                            .background(accentColor)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Código de verificación dinámico de 6 dígitos. Díctalo al emprendedor solo al recibir tu pedido en mano.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ValidarPinEntregaDialog(
    orden: Orden,
    onConfirmarPin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pinIngresado by remember { mutableStateOf("") }
    var errorPin by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Validar Entrega con PIN",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Solicita al comprador el código dinámico de 6 dígitos que aparece en su pedido ${orden.codigoOrden}:",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinIngresado,
                    onValueChange = { nuevo ->
                        val limpio = nuevo.filter { it.isDigit() }.take(6)
                        pinIngresado = limpio
                        errorPin = null
                    },
                    placeholder = {
                        Text(
                            text = "000000",
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 4.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorPin != null,
                    supportingText = errorPin?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val okPress = rememberPressInteraction()
            Button(
                onClick = {
                    if (CodigoEntregaUtil.validarCodigoEntrega(orden.id, orden.codigoOrden, pinIngresado)) {
                        onConfirmarPin(pinIngresado)
                    } else {
                        errorPin = "Código incorrecto o vencido. Pide al comprador el PIN vigente de 6 dígitos."
                    }
                },
                enabled = pinIngresado.length == 6,
                interactionSource = okPress,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                modifier = Modifier.pressScale(okPress)
            ) {
                Text("Confirmar Entrega", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
