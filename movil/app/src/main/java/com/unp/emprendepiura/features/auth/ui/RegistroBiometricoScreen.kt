package com.unp.emprendepiura.features.auth.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.features.auth.ui.components.EnrollmentFaceCard
import com.unp.emprendepiura.features.auth.ui.components.EnrollmentFingerprintCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroBiometricoScreen(
    dni: String,
    nombre: String,
    telefono: String,
    correo: String,
    zonaPiura: String,
    contrasena: String,
    huellaVinculada: Boolean,
    fotoFacialUri: Uri?,
    rostroVerificado: Boolean,
    analizandoRostro: Boolean,
    saving: Boolean,
    esModoEdicion: Boolean = false,
    buscandoUsuario: Boolean = false,
    dniError: String? = null,
    onDniChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onZonaChange: (String) -> Unit,
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
                            text = if (esModoEdicion) "Editar Perfil y Biometría" else "Crear Cuenta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (esModoEdicion) "Actualiza tus datos, huella o rostro" else "Únete al Marketplace local de Piura",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dni,
                        onValueChange = onDniChange,
                        enabled = !saving && !esModoEdicion,
                        isError = dniError != null,
                        label = { Text("DNI *") },
                        placeholder = { Text("8 dígitos numéricos") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (buscandoUsuario) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "${dni.length}/8",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                            }
                        },
                        supportingText = dniError?.let { err ->
                            {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.5.sp
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (esModoEdicion) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Editando tu cuenta verificada. Puedes actualizar tus datos, huella o rostro.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

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

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = onTelefonoChange,
                        label = { Text("Celular / WhatsApp") },
                        placeholder = { Text("Ej. 969123456") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = correo,
                        onValueChange = onCorreoChange,
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("Ej. usuario@unp.edu.pe") },
                        leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Zona en Piura / UNP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PiuraConstants.ZONAS_PIURA.forEach { zona ->
                                FilterChip(
                                    selected = zonaPiura == zona,
                                    onClick = { onZonaChange(zona) },
                                    label = { Text(zona, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contrasena,
                        onValueChange = onContrasenaChange,
                        label = { Text(if (esModoEdicion) "Nueva Contraseña (opcional)" else "Contraseña *") },
                        placeholder = {
                            Text(if (esModoEdicion) "Vacío para conservar la actual" else "Mínimo 4 caracteres")
                        },
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    EnrollmentFingerprintCard(
                        vinculada = huellaVinculada,
                        onVincularClick = onVincularHuella
                    )

                    EnrollmentFaceCard(
                        fotoUri = fotoFacialUri,
                        rostroVerificado = rostroVerificado,
                        analizando = analizandoRostro,
                        onCapturarClick = onCapturarRostro
                    )
                }
            }

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
                    enabled = !saving && dniError == null,
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
                            text = if (esModoEdicion) "Actualizar Datos" else "Crear Cuenta",
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
