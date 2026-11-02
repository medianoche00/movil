package com.unp.registroestudiantes.biometria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometrico

/*
 * Archivo: PostLoginScreen.kt
 * Proposito: Pantalla destino tras iniciar sesion ("Tema · En Construccion"), mostrando
 *            los datos del usuario autenticado, acceso al Panel Academico (Trabajo N°03) y Salir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostLoginScreen(
    usuario: UsuarioBiometrico?,
    metodoAutenticacion: String,
    onIrAPanelAcademico: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nombreMostrar = usuario?.nombre ?: "Usuario Autenticado"
    val dniMostrar = usuario?.dni ?: "--------"
    val iniciales = usuario?.iniciales ?: "US"
    val fotoUrl = usuario?.imagenFacial?.takeIf { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tema",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Sesión Activa · UNP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onCerrarSesion,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Salir", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Tarjeta de Identidad del Usuario Verificado
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fotoUrl != null) {
                            AsyncImage(
                                model = fotoUrl,
                                contentDescription = "Fotografía facial de $nombreMostrar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = iniciales,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = nombreMostrar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "DNI: $dniMostrar",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = metodoAutenticacion.ifBlank { "Sesión Verificada" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. Bloque Central "En Construccion" (Fiel al boceto del Trabajo N°04)
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "En Construcción",
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "El control de acceso biométrico se completó correctamente. El módulo principal del tema se encuentra en construcción.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3. Acciones Inferiores: Panel Academico (Trabajo N°03) y Salir
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onIrAPanelAcademico,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ir al Panel Académico (Trabajo N° 03)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onCerrarSesion,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Salir de la Sesión",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
