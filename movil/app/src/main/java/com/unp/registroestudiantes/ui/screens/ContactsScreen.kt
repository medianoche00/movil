package com.unp.registroestudiantes.ui.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unp.registroestudiantes.data.Contact
import com.unp.registroestudiantes.ui.theme.pressScale

/*
 * Archivo: ContactsScreen.kt
 * Proposito: Interfaz para gestionar la agenda de contactos con validaciones robustas.
 *
 * Guia de transicion Desarrollo Web (HTML/JS) -> Jetpack Compose:
 *
 * 1. Formularios Controlados:
 *    En React usariamos estados para los valores de los 'input'. Aqui usamos
 *    'var nameInput by remember { mutableStateOf("") }' de manera analoga.
 *
 * 2. Validaciones Reactivas:
 *    Evaluamos la validez de los campos en tiempo real (isNameValid, etc.) basados
 *    en el estado actual. Los errores solo se muestran si el usuario interactua o intenta enviar
 *    (isSubmittedAttempted), tal como se hace con la propiedad 'touched' en Formik.
 *
 * 3. Animaciones de Entrada Escalonadas:
 *    Se aplican retardos secuenciales a cada tarjeta de la lista basandose en su indice,
 *    simulando las transiciones fluidas comunes en el ecosistema Apple (iOS).
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    onAddContact: (name: String, phone: String, email: String) -> Unit,
    onDeleteContact: (id: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var isSubmittedAttempted by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // Validaciones robustas
    val namePattern = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$".toRegex()
    val isNameValid = nameInput.trim().length >= 3 && nameInput.matches(namePattern)
    val isPhoneValid = phoneInput.trim().length == 9 && phoneInput.trim().all { it.isDigit() }
    val isEmailValid = emailInput.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches()
    val isFormValid = isNameValid && isPhoneValid && isEmailValid

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Agenda de Contactos")
                        Text(
                            "${contacts.size} registrados",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Nuevo Contacto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nombre Completo") },
                            placeholder = { Text("Ej. Juan Pérez") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = isSubmittedAttempted && !isNameValid,
                            supportingText = {
                                if (isSubmittedAttempted && !isNameValid) {
                                    Text(
                                        "Debe tener al menos 3 caracteres y solo letras.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }
                                if (digitsOnly.length <= 9) {
                                    phoneInput = digitsOnly
                                }
                            },
                            label = { Text("Celular (9 dígitos)") },
                            placeholder = { Text("Ej. 987654321") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = isSubmittedAttempted && !isPhoneValid,
                            supportingText = {
                                if (isSubmittedAttempted && !isPhoneValid) {
                                    Text(
                                        "Debe contener exactamente 9 dígitos numéricos.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Correo Electrónico") },
                            placeholder = { Text("Ej. contacto@gmail.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = isSubmittedAttempted && !isEmailValid,
                            supportingText = {
                                if (isSubmittedAttempted && !isEmailValid) {
                                    Text(
                                        "Ingrese una dirección de correo válida.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                isSubmittedAttempted = true
                                if (isFormValid) {
                                    onAddContact(nameInput, phoneInput, emailInput)
                                    nameInput = ""
                                    phoneInput = ""
                                    emailInput = ""
                                    isSubmittedAttempted = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .pressScale()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar Contacto")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Buscar por nombre...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isBlank()) "La agenda está vacía"
                                else "Sin coincidencias para \"\$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredContacts, key = { _, c -> c.id }) { index, contact ->
                    ContactCardItem(
                        index = index,
                        contact = contact,
                        onDelete = { contactToDelete = contact }
                    )
                }
            }
        }
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Eliminar contacto") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${contact.name}? Esta acción no se puede deshacer.") },
            shape = RoundedCornerShape(20.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteContact(contact.id)
                        contactToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ContactCardItem(
    index: Int,
    contact: Contact,
    onDelete: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(minOf(index, 8) * 40L)
        shown = true
    }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .pressScale()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = contact.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = contact.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = contact.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar contacto",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
