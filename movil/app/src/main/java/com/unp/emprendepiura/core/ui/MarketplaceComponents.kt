package com.unp.emprendepiura.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

fun iconoPorCategoria(nombreOCodigo: String): ImageVector {
    val norm = nombreOCodigo.lowercase()
    return when {
        norm.contains("fastfood") || norm.contains("alimento") || norm.contains("postre") -> Icons.Default.Fastfood
        norm.contains("menu_book") || norm.contains("apunte") || norm.contains("papeler") -> Icons.AutoMirrored.Filled.MenuBook
        norm.contains("checkroom") || norm.contains("moda") || norm.contains("ropa") -> Icons.Default.Checkroom
        norm.contains("devices") || norm.contains("tecnolog") -> Icons.Default.Devices
        norm.contains("palette") || norm.contains("artesan") -> Icons.Default.Palette
        norm.contains("build") || norm.contains("servicio") -> Icons.Default.Build
        else -> Icons.Default.Inventory2
    }
}

@Composable
fun ProductThumbnail(
    imageUrl: String,
    categoria: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = iconoPorCategoria(categoria),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun StatusPillBadge(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyStateBox(
    icon: ImageVector,
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    accionTexto: String? = null,
    onAccionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = titulo,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitulo,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (accionTexto != null && onAccionClick != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAccionClick,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(accionTexto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}
