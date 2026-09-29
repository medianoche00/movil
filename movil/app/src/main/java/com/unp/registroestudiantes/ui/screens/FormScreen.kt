package com.unp.registroestudiantes.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Student
import com.unp.registroestudiantes.util.Media
import com.unp.registroestudiantes.viewmodel.FormInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    editing: Student?,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (FormInput) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var age by remember { mutableStateOf(editing?.age?.toString().orEmpty()) }
    var career by remember { mutableStateOf(editing?.career.orEmpty()) }
    var email by remember { mutableStateOf(editing?.email.orEmpty()) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cvUri by remember { mutableStateOf<Uri?>(null) }
    var cvName by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }
    val cvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        cvUri = it; cvName = it?.let { u -> Media.displayName(ctx, u) }
    }

    val nameError = name.isBlank()
    val ageInt = age.toIntOrNull()
    val ageError = ageInt == null || ageInt <= 0
    val careerError = career.isBlank()
    val canSave = !nameError && !ageError && !careerError && !saving

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (editing == null) "Nuevo estudiante" else "Editar estudiante") },
            navigationIcon = { TextButton(onClick = onCancel) { Text("Cancelar") } }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Foto
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = !saving) { photoPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val shown = photoUri ?: editing?.photoUrl
                if (shown != null) {
                    AsyncImage(shown, "Foto", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Toca para elegir una foto", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nombre") },
                isError = nameError, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = age, onValueChange = { age = it.filter(Char::isDigit) }, label = { Text("Edad") },
                isError = ageError, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = career, onValueChange = { career = it }, label = { Text("Carrera") },
                isError = careerError, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email (opcional)") },
                singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedCard(
                onClick = { if (!saving) cvPicker.launch("*/*") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("CV (PDF o imagen, opcional)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            cvName ?: (if (editing?.cvUrl != null) "Ya tiene un CV adjunto" else "Toca para adjuntar"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(
                        FormInput(
                            name = name.trim(), age = ageInt ?: 0, career = career.trim(),
                            email = email.trim().ifBlank { null },
                            photoBytes = photoUri?.let { Media.compressImage(ctx, it) },
                            cvUri = cvUri
                        )
                    )
                },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Generando reseña…")
                } else {
                    Text("Guardar")
                }
            }
        }
    }
}
