package com.unp.registroestudiantes.biometria.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/*
 * Archivo: BiometricMethodCards.kt
 * Proposito: Tarjetas modulares para el registro de Huella Dactilar y Reconocimiento Facial
 *            en el formulario de nuevo usuario.
 */

@Composable
fun EnrollmentFingerprintCard(
    vinculada: Boolean,
    onVincularClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (vinculada) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = if (vinculada) Color(0xFF2E7D32).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (vinculada) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Huella Dactilar *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (vinculada) "Huella vinculada (Token listo)" else "Pendiente de vincular",
                    fontSize = 12.sp,
                    color = if (vinculada) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onVincularClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (vinculada) "Vinculada" else "Vincular",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EnrollmentFaceCard(
    fotoUri: Uri?,
    rostroVerificado: Boolean,
    analizando: Boolean,
    onCapturarClick: () -> Unit,
    onAmpliarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = if (rostroVerificado) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = 1.dp,
            color = if (rostroVerificado) Color(0xFF2E7D32).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        if (fotoUri != null && onAmpliarClick != null) {
                            onAmpliarClick()
                        } else {
                            onCapturarClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUri != null && rostroVerificado) {
                    AsyncImage(
                        model = fotoUri,
                        contentDescription = "Rostro capturado",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reconocimiento Facial *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        analizando -> "Analizando rostro con ML Kit..."
                        rostroVerificado -> "Rostro verificado y listo"
                        else -> "Sin imagen capturada"
                    },
                    fontSize = 12.sp,
                    color = if (rostroVerificado) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onCapturarClick,
                enabled = !analizando,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (analizando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (rostroVerificado) "Recapturar" else "Capturar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
