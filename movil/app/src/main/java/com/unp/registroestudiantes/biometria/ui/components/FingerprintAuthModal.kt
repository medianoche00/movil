package com.unp.registroestudiantes.biometria.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unp.registroestudiantes.biometria.util.BiometricPromptHelper
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Archivo: FingerprintAuthModal.kt
 * Proposito: Modal de autenticacion y vinculacion de huella dactilar que muestra el estado
 *            de lectura, exito en verde y errores en rojo directamente dentro del modal,
 *            con botones de Cancelar y Reintentar (sin disparar el banner rojo externo).
 */
enum class EstadoHuellaModal {
    ESPERANDO,
    ERROR,
    EXITO
}

@Composable
fun FingerprintAuthModal(
    visible: Boolean,
    esModoRegistro: Boolean,
    dniObjetivo: String,
    subtituloUsuario: String,
    errorInicial: String? = null,
    onHuellaVerificada: (tokenHuella: String) -> Unit,
    onCancelar: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    var estado by remember(visible, errorInicial) {
        mutableStateOf(if (errorInicial != null) EstadoHuellaModal.ERROR else EstadoHuellaModal.ESPERANDO)
    }
    var mensajeDetalle by remember(visible, errorInicial) {
        mutableStateOf(errorInicial ?: "Coloca tu dedo sobre el lector biométrico del teléfono")
    }
    var intentoTrigger by remember(visible) { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }

    // Animacion de sacudida horizontal cuando falla la huella dentro del modal
    LaunchedEffect(estado, intentoTrigger) {
        if (estado == EstadoHuellaModal.ERROR) {
            val offsets = listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f)
            for (target in offsets) {
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 45))
            }
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    // Dispara el BiometricPrompt del dispositivo cuando estamos en estado ESPERANDO
    LaunchedEffect(visible, intentoTrigger, errorInicial) {
        if (visible && errorInicial == null && estado == EstadoHuellaModal.ESPERANDO) {
            delay(180)
            BiometricPromptHelper.autenticarHuella(
                context = context,
                titulo = if (esModoRegistro) "Vincular Huella Dactilar" else "Verificación de Huella",
                subtitulo = subtituloUsuario,
                dni = dniObjetivo,
                onExito = { token ->
                    estado = EstadoHuellaModal.EXITO
                    mensajeDetalle = "¡Huella verificada correctamente!"
                    onHuellaVerificada(token)
                },
                onError = { err ->
                    estado = EstadoHuellaModal.ERROR
                    mensajeDetalle = err
                }
            )
        }
    }

    val accentColor = when (estado) {
        EstadoHuellaModal.ESPERANDO -> Color(0xFF2563EB)
        EstadoHuellaModal.ERROR -> Color(0xFFEF4444)
        EstadoHuellaModal.EXITO -> Color(0xFF10B981)
    }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicador superior tipo BottomSheet
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (esModoRegistro) "Vincular Huella Dactilar" else "Verificación de Huella",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = subtituloUsuario,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(22.dp))

                    // Circulo central del sensor con animacion de sacudida en caso de error
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(2.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (estado) {
                                EstadoHuellaModal.EXITO -> Icons.Default.Check
                                else -> Icons.Default.Fingerprint
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Mensaje de estado o recuadro de error interno del modal
                    when (estado) {
                        EstadoHuellaModal.ESPERANDO -> {
                            Text(
                                text = "Esperando lectura del sensor...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = mensajeDetalle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        EstadoHuellaModal.EXITO -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = mensajeDetalle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF10B981),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }

                        EstadoHuellaModal.ERROR -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = mensajeDetalle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // Botones inferiores: Cancelar y Reintentar
                    if (estado != EstadoHuellaModal.EXITO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelar,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 13.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Cancelar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            if (errorInicial == null) {
                                Button(
                                    onClick = {
                                        estado = EstadoHuellaModal.ESPERANDO
                                        mensajeDetalle = "Coloca tu dedo sobre el lector biométrico del teléfono"
                                        intentoTrigger++
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(vertical = 13.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Reintentar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
