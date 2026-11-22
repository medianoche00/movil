package com.unp.emprendepiura.features.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.Mensaje

// Colores identicos a WhatsApp y Telegram
val WhatsAppOutgoingLight = Color(0xFFDCF8C6)
val WhatsAppOutgoingDark = Color(0xFF005C4B)
val WhatsAppIncomingLight = Color(0xFFFFFFFF)
val WhatsAppIncomingDark = Color(0xFF1F2C34)
val WhatsAppBgLight = Color(0xFFEFEAE2)
val WhatsAppBgDark = Color(0xFF0B141A)
val WhatsAppDoubleCheckBlue = Color(0xFF53BDEB)
val WhatsAppGreenPrimary = Color(0xFF00A884)

/**
 * Indicador animado de 3 puntos saltarines ("Escribiendo...") identico a WhatsApp / Telegram.
 */
@Composable
fun TypingIndicatorDots(
    modifier: Modifier = Modifier,
    puntoColor: Color = Color(0xFF00A884),
    tamanoPunto: Float = 7f
) {
    val transition = rememberInfiniteTransition(label = "typingTransition")

    val offset1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val offset2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val offset3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = offset1.dp)
                .size(tamanoPunto.dp)
                .clip(CircleShape)
                .background(puntoColor)
        )
        Box(
            modifier = Modifier
                .offset(y = offset2.dp)
                .size(tamanoPunto.dp)
                .clip(CircleShape)
                .background(puntoColor)
        )
        Box(
            modifier = Modifier
                .offset(y = offset3.dp)
                .size(tamanoPunto.dp)
                .clip(CircleShape)
                .background(puntoColor)
        )
    }
}

/**
 * Burbuja animada en la lista que indica que la contraparte esta escribiendo en vivo.
 */
@Composable
fun TypingBubbleMessage(
    nombreEmprendedor: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp),
            color = if (isDark) WhatsAppIncomingDark else WhatsAppIncomingLight,
            shadowElevation = 1.5.dp,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$nombreEmprendedor está escribiendo",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TypingIndicatorDots(tamanoPunto = 6f)
            }
        }
    }
}

/**
 * Burbuja de mensaje estilo WhatsApp / Telegram con timestamp integrado y checks azules.
 */
@Composable
fun WhatsAppChatBubble(
    mensaje: Mensaje,
    esMio: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bubbleColor = if (esMio) {
        if (isDark) WhatsAppOutgoingDark else WhatsAppOutgoingLight
    } else {
        if (isDark) WhatsAppIncomingDark else WhatsAppIncomingLight
    }

    val bubbleShape = if (esMio) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 3.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 16.dp)
    }

    val hora = mensaje.creadoEn.drop(11).take(5).ifBlank { "Ahora" }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = if (isDark) 0.dp else 1.2.dp,
            modifier = Modifier.widthIn(min = 72.dp, max = 295.dp)
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 6.dp)) {
                Text(
                    text = mensaje.contenido,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    color = if (isDark) {
                        if (esMio) Color(0xFFE9EDEF) else Color(0xFFE9EDEF)
                    } else {
                        Color(0xFF111B21)
                    }
                )

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = hora,
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFF8696A0) else Color(0xFF667781)
                    )
                    if (esMio) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Entregado y leído",
                            tint = WhatsAppDoubleCheckBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Separador de fecha flotante estilo WhatsApp ("Hoy", "Ayer", etc.).
 */
@Composable
fun ChatDateBadge(texto: String = "Hoy") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isSystemInDarkTheme()) Color(0xFF182229) else Color(0xFFE1F3FB),
            shadowElevation = 0.5.dp
        ) {
            Text(
                text = texto,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF54656F),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Modal para visualizar el Perfil Completo del Negocio / Emprendimiento (Piura UNP).
 */
@Composable
fun PerfilNegocioDialog(
    tienda: Emprendimiento?,
    nombreFallback: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun copiarAlPortapapeles(label: String, valor: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, valor))
        Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar grande con badge de verificacion
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (tienda?.nombreTienda ?: nombreFallback).take(1).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tienda?.nombreTienda ?: nombreFallback,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Emprendedor Verificado",
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${tienda?.categoriaPrincipal ?: "Emprendimiento Piura"} · ${tienda?.zonaPiura ?: "Campus UNP"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Informacion clave del negocio
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!tienda?.descripcion.isNullOrBlank()) {
                        Text(
                            text = tienda!!.descripcion,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Punto de encuentro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Punto de entrega habitual:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tienda?.puntoEncuentroHabitual ?: "Campus Universitario UNP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Telefono / WhatsApp
                    val tel = tienda?.telefonoContacto.orEmpty()
                    if (tel.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copiarAlPortapapeles("Teléfono", tel) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Phone, null, tint = Color(0xFF00A884), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Contacto WhatsApp:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(onClick = { copiarAlPortapapeles("Teléfono", tel) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Numero Yape / Plin
                    val yape = tienda?.numeroYapePlin.orEmpty()
                    if (yape.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copiarAlPortapapeles("Yape/Plin", yape) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Payments, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Pago Yape / Plin:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(yape, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(onClick = { copiarAlPortapapeles("Yape/Plin", yape) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}
