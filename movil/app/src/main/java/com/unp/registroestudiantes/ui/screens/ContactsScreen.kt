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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    onAddContact: (name: String, phone: String, email: String) -> Unit,
    onDeleteContact: (id: String) -> Unit
) {
    // Estados para los campos del formulario
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    // Estado para saber si el usuario ya interactuó con el formulario
    var isSubmittedAttempted by remember { mutableStateOf(false) }

    // Estado para la búsqueda por nombre
    var searchQuery by remember { mutableStateOf("") }

    // Estado para el contacto a eliminar
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // --- VALIDACIONES ---
    val isNameValid = nameInput.trim().length >= 3
    val isPhoneValid = phoneInput.trim().length == 9 && phoneInput.trim().all { it.isDigit() }
    val isEmailValid = emailInput.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches()
    val isFormValid = isNameValid && isPhoneValid && isEmailValid

    // Filtrar la lista SOLO por Nombre
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lista de Contactos")
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
            // ==========================================
            // SECCIÓN 1: FORMULARIO DE REGISTRO
            // ==========================================
            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Registrar Contacto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Campo Nombre
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nombre") },
                            placeholder = { Text("Ej. Juan Pérez") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            isError = isSubmittedAttempted && !isNameValid,
                            supportingText = {
                                if (isSubmittedAttempted && !isNameValid) {
                                    Text("El nombre debe tener al menos 3 letras", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 2. Campo Celular (Máximo 9 dígitos numéricos)
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
                            isError = isSubmittedAttempted && !isPhoneValid,
                            supportingText = {
                                if (isSubmittedAttempted && !isPhoneValid) {
                                    Text("El celular debe tener exactamente 9 dígitos", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 3. Campo Correo
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Correo") },
                            placeholder = { Text("Ej. contacto@gmail.com") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            isError = isSubmittedAttempted && !isEmailValid,
                            supportingText = {
                                if (isSubmittedAttempted && !isEmailValid) {
                                    Text("Ingrese un correo electrónico válido", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4. Botón Agregar
                        Button(
                            onClick = {
                                isSubmittedAttempted = true
                                if (isFormValid) {
                                    onAddContact(nameInput, phoneInput, emailInput)
                                    // Limpiar campos del formulario
                                    nameInput = ""
                                    phoneInput = ""
                                    emailInput = ""
                                    isSubmittedAttempted = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Agregar")
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 2: BUSCAR POR NOMBRE
            // ==========================================
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Buscar por Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==========================================
            // SECCIÓN 3: LISTA DE CONTACTOS
            // ==========================================
            if (filteredContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isBlank()) "No hay contactos en la lista"
                                else "No se encontró ningún contacto con el nombre \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
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

    // Diálogo de confirmación para eliminar
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("¿Eliminar contacto?") },
            text = { Text("¿Deseas eliminar a ${contact.name} de la lista?") },
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

/**
 * Componente individual para mostrar la tarjeta de cada contacto.
 * Muestra: Nombre, Celular, Correo y Botón de Eliminar (NO EDITAR).
 */
@Composable
private fun ContactCardItem(
    index: Int,
    contact: Contact,
    onDelete: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(minOf(index, 8) * 30L)
        shown = true
    }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 4 }
    ) {
        ElevatedCard(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con la letra inicial
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Información: Nombre, Celular y Correo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
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
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = contact.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botón SOLO Eliminar (NO EDITAR)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar contacto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
