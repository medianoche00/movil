package com.unp.registroestudiantes.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Student
import com.unp.registroestudiantes.ui.theme.pressScale
import com.unp.registroestudiantes.util.Media
import com.unp.registroestudiantes.viewmodel.FormInput

/*
 * Archivo: FormScreen.kt
 * Proposito: Formulario de captura y edicion de datos del estudiante.
 *
 * Guia de transicion HTML Forms / Java Android -> Jetpack Compose:
 *
 * 1. Activity Result API con 'rememberLauncherForActivityResult':
 *    En Android clasico con Java se utilizaba 'startActivityForResult(intent, REQUEST_CODE)'
 *    y se interceptaba en 'onActivityResult(...)', lo cual requeria codigos numericos
 *    magicos y era propenso a errores.
 *    En Compose, 'rememberLauncherForActivityResult(ActivityResultContracts.GetContent())'
 *    abre el selector de archivos del sistema y entrega el Uri en una lambda limpia.
 *
 * 2. Patron de Validacion Diferida (UX Friendly):
 *    Un error comun de experiencia de usuario es mostrar bordes rojos de error inmediatamente
 *    al abrir un formulario en blanco.
 *    Aqui se usan banderas 'touched' y 'attemptedSave'. El mensaje de error solo se dibuja
 *    si el usuario ya interactuo con el campo y lo dejo vacio, o si intento pulsar 'Guardar'.
 *
 * 3. Teclados virtuales adaptados (KeyboardOptions):
 *    - KeyboardType.Number: Para la edad (abre el teclado numerico).
 *    - KeyboardType.Email: Para el correo electronico (incluye arroba y punto com).
 *    - KeyboardCapitalization.Words: Pone mayuscula automatica en cada palabra de nombres y carreras.
 *
 * 4. Desplazamiento reactivo con 'verticalScroll':
 *    Al agregar '.verticalScroll(rememberScrollState())' a la columna, la pantalla
 *    se desplaza automaticamente cuando el teclado virtual aparece, evitando que
 *    el boton Guardar quede tapado.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    editing: Student?,
    saving: Boolean,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onCancel: () -> Unit,
    onSave: (FormInput) -> Unit
) {
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra errores de guardado en el Snackbar cuando ocurren
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    // Estados de los campos de texto
    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var age by remember { mutableStateOf(editing?.age?.toString().orEmpty()) }
    var career by remember { mutableStateOf(editing?.career.orEmpty()) }
    var email by remember { mutableStateOf(editing?.email.orEmpty()) }

    // Estados de archivos seleccionados
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cvUri by remember { mutableStateOf<Uri?>(null) }
    var cvName by remember { mutableStateOf<String?>(null) }

    // Banderas de interaccion para control de validacion
    var nameTouched by remember { mutableStateOf(false) }
    var ageTouched by remember { mutableStateOf(false) }
    var careerTouched by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }

    // Lanzadores de intents para seleccionar foto y documento
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        photoUri = it
    }
    val cvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        cvUri = it
        cvName = it?.let { u -> Media.displayName(ctx, u) }
    }

    // Reglas de validacion
    val ageInt = age.toIntOrNull()
    val isNameValid = name.isNotBlank()
    val isAgeValid = ageInt != null && ageInt > 0
    val isCareerValid = career.isNotBlank()
    val canSave = isNameValid && isAgeValid && isCareerValid && !saving

    // Los errores visuales SOLO se muestran si el usuario toco el campo o intento guardar
    val showNameError = (nameTouched || attemptedSave) && !isNameValid
    val showAgeError = (ageTouched || attemptedSave) && !isAgeValid
    val showCareerError = (careerTouched || attemptedSave) && !isCareerValid

    val fieldShape = RoundedCornerShape(14.dp)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editing == null) "Nuevo estudiante" else "Editar estudiante",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar", style = MaterialTheme.typography.bodyLarge)
                    }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Seccion del Avatar con insignia de camara flotante
            val shownPhoto = photoUri ?: editing?.photoUrl
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .pressScale()
                    .clickable(enabled = !saving) { photoPicker.launch("image/*") }
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                        .border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (shownPhoto != null) {
                        AsyncImage(
                            model = shownPhoto,
                            contentDescription = "Foto elegida",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        )
                    }
                }

                // Insignia circular de camara
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(34.dp)
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Cambiar foto",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Text(
                text = if (shownPhoto != null) "Toca la foto para cambiarla" else "Toca para agregar foto",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            // Campo: Nombre
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameTouched = true
                },
                label = { Text("Nombre completo *") },
                placeholder = { Text("Ej. Marco Pérez") },
                leadingIcon = {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                },
                isError = showNameError,
                supportingText = if (showNameError) {
                    { Text("El nombre es obligatorio") }
                } else null,
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            // Campo: Edad
            OutlinedTextField(
                value = age,
                onValueChange = {
                    age = it.filter(Char::isDigit)
                    ageTouched = true
                },
                label = { Text("Edad *") },
                placeholder = { Text("Ej. 21") },
                leadingIcon = {
                    Icon(Icons.Default.Cake, null, tint = MaterialTheme.colorScheme.primary)
                },
                isError = showAgeError,
                supportingText = if (showAgeError) {
                    { Text("Ingresa una edad válida (mayor a 0)") }
                } else null,
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Campo: Carrera
            OutlinedTextField(
                value = career,
                onValueChange = {
                    career = it
                    careerTouched = true
                },
                label = { Text("Carrera *") },
                placeholder = { Text("Ej. Ingeniería Informática") },
                leadingIcon = {
                    Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                },
                isError = showCareerError,
                supportingText = if (showCareerError) {
                    { Text("La carrera es obligatoria") }
                } else null,
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            // Campo: Email (Opcional)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (opcional)") },
                placeholder = { Text("estudiante@unp.edu.pe") },
                leadingIcon = {
                    Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // Tarjeta para adjuntar CV
            val hasCv = cvName != null || editing?.cvUrl != null
            ElevatedCard(
                onClick = { if (!saving) cvPicker.launch("*/*") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (hasCv)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasCv) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (hasCv) Icons.Default.CheckCircle else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (hasCv) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Currículum Vitae (CV)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = cvName
                                ?: (if (editing?.cvUrl != null) "CV adjuntado anteriormente" else "Toca para subir PDF o imagen"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasCv) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Boton Principal de Guardado
            Button(
                onClick = {
                    attemptedSave = true
                    if (canSave) {
                        onSave(
                            FormInput(
                                name = name.trim(),
                                age = ageInt ?: 0,
                                career = career.trim(),
                                email = email.trim().ifBlank { null },
                                photoBytes = photoUri?.let { Media.compressImage(ctx, it) },
                                cvUri = cvUri
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressScale()
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Analizando y guardando…", fontWeight = FontWeight.Medium)
                } else {
                    Text("Guardar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
