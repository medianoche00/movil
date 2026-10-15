package com.unp.registroestudiantes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.registroestudiantes.ui.theme.pressScale

/*
 * Archivo: MenuScreen.kt
 * Proposito: Pantalla de inicio y selector de modulos principales despues del Splash Screen.
 *
 * Guia de navegacion y arquitectura:
 * 1. Punto de entrada funcional: Permite al usuario elegir entre el modulo
 *    de Registro de Estudiantes (con analisis de CV y resenas) y la Agenda de Contactos.
 * 2. Experiencia de usuario (UX):
 *    - Tarjetas interactivas con respuesta tactil mediante la animacion appleSpring (.pressScale).
 *    - Indicadores numericos (badges) que muestran la cantidad de registros en cada modulo.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    studentsCount: Int,
    contactsCount: Int,
    onOpenStudents: () -> Unit,
    onOpenContacts: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Panel Principal",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera institucional
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Text(
                        text = "Universidad Nacional de Piura",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Sistema Académico",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Selecciona el módulo al que deseas acceder",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Boton / Tarjeta 1: Registro de Estudiantes (DESHABILITADO)
            ModuleCard(
                title = "Registro de Estudiantes",
                subtitle = "Módulo temporalmente fuera de servicio. Acceso deshabilitado.",
                badgeText = "Deshabilitado",
                icon = Icons.Default.School,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                enabled = false,
                onClick = {}
            )

            Spacer(Modifier.height(16.dp))

            // Boton / Tarjeta 2: Agenda de Contactos (ACTIVO)
            ModuleCard(
                title = "Agenda de Contactos",
                subtitle = "Directorio telefónico y correos electrónicos institucionales de contacto rápido.",
                badgeText = "$contactsCount contactos",
                icon = Icons.Default.ContactPhone,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary,
                enabled = true,
                onClick = onOpenContacts
            )

            Spacer(Modifier.height(32.dp))

            // Pie institucional
            Text(
                text = "Sistema de Gestión UNP · Versión 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta interactiva de modulo con icono tematico, descripcion y respuesta tactil opcional.
 */
@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cardModifier = if (enabled) {
        Modifier
            .fillMaxWidth()
            .pressScale()
    } else {
        Modifier.fillMaxWidth()
    }

    ElevatedCard(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = cardModifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contenedor circular del icono
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) containerColor 
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (enabled) containerColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (enabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Abrir módulo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                lineHeight = 20.sp
            )
        }
    }
}
