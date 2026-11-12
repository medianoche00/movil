package com.unp.registroestudiantes.biometria.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.registroestudiantes.biometria.ui.components.EnrollmentFaceCard
import com.unp.registroestudiantes.biometria.ui.components.EnrollmentFingerprintCard

/*
 * Archivo: RegistroBiometricoScreen.kt
 * Proposito: Formulario de enrolamiento biometrico de usuario (DNI, Nombre, Contrasena,
 *            Huella Dactilar e Imagen para Reconocimiento Facial).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroBiometricoScreen(
    dni: String,
    nombre: String,
    contrasena: String,
    huellaVinculada: Boolean,
    fotoFacialUri: Uri?,
    rostroVerificado: Boolean,
    analizandoRostro: Boolean,
    saving: Boolean,
    esModoEdicion: Boolean = false,
    buscandoUsuario: Boolean = false,
    onDniChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onVincularHuella: () -> Unit,
    onCapturarRostro: () -> Unit,
    onGuardarUsuario: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarContrasena by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (esModoEdicion) "Modificar Datos Biométricos" else "Registrar Usuario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (esModoEdicion) "Actualiza tu huella, rostro o datos" else "Completa tus datos y biometría",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. DNI
                    OutlinedTextField(
                        value = dni,
                        onValueChange = onDniChange,
                        label = { Text("DNI *") },
                        placeholder = { Text("8 dígitos numéricos") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (buscandoUsuario) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "${dni.length}/8",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Aviso cuando se edita un usuario existente
                    if (esModoEdicion) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Usuario encontrado: $nombre. Puedes volver a capturar tu rostro, cambiar tu huella o modificar tus datos.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 2. Nombre
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = onNombreChange,
                        label = { Text("Nombre Completo *") },
                        placeholder = { Text("Ej. Ana Lucía Torres") },
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Contrasena
                    OutlinedTextField(
                        value = contrasena,
                        onValueChange = onContrasenaChange,
                        label = { Text("Contraseña *") },
                        placeholder = { Text("Mínimo 4 caracteres") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                                Icon(
                                    imageVector = if (mostrarContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Mostrar u ocultar contraseña",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // 4. Tarjeta de Enrolamiento de Huella Dactilar
                    EnrollmentFingerprintCard(
                        vinculada = huellaVinculada,
                        onVincularClick = onVincularHuella,
                        esModoEdicion = esModoEdicion
                    )

                    // 5. Tarjeta de Imagen para Reconocimiento Facial
                    EnrollmentFaceCard(
                        fotoUri = fotoFacialUri,
                        rostroVerificado = rostroVerificado,
                        analizando = analizandoRostro,
                        onCapturarClick = onCapturarRostro
                    )
                }
            }

            // Botones inferiores Cancelar y Guardar/Actualizar Usuario
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onVolver,
                    enabled = !saving,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Button(
                    onClick = onGuardarUsuario,
                    enabled = !saving,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (esModoEdicion) "Actualizando..." else "Guardando...",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = if (esModoEdicion) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (esModoEdicion) "Actualizar Datos" else "Guardar Usuario",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
