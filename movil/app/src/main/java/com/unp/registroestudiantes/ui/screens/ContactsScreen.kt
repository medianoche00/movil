package com.unp.registroestudiantes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.unp.registroestudiantes.data.Contact
import com.unp.registroestudiantes.ui.theme.pressScale
import com.unp.registroestudiantes.util.DeviceContactsUtil
import com.unp.registroestudiantes.viewmodel.ContactViewModel
import kotlinx.coroutines.delay

/*
 * Archivo: ContactsScreen.kt
 * Proposito: Interfaz para la Agenda de Contactos con sincronizacion
 * nativa del dispositivo y persistencia en la nube.
 * Diseno moderno estilo Apple con notificaciones dinamicas flotantes (Dynamic Island style).
 */

private data class CountryOption(val flag: String, val code: String, val dialCode: String, val name: String)

private val COUNTRY_OPTIONS = listOf(
    CountryOption("🇵🇪", "PE", "+51", "Perú"),
    CountryOption("🇦🇷", "AR", "+54", "Argentina"),
    CountryOption("🇨🇱", "CL", "+56", "Chile"),
    CountryOption("🇨🇴", "CO", "+57", "Colombia"),
    CountryOption("🇪🇨", "EC", "+593", "Ecuador"),
    CountryOption("🇲🇽", "MX", "+52", "México"),
    CountryOption("🇪🇸", "ES", "+34", "España"),
    CountryOption("🇺🇸", "US", "+1", "Estados Unidos"),
    CountryOption("🇧🇴", "BO", "+591", "Bolivia"),
    CountryOption("🇻🇪", "VE", "+58", "Venezuela")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Estados locales del formulario
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(COUNTRY_OPTIONS[0]) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var syncToPhone by remember { mutableStateOf(false) }

    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // Mensaje de alerta dinamica flotante
    var activeErrorMessage by remember { mutableStateOf<String?>(null) }

    // Temporizador de auto-cierre para el mensaje de error (desaparece solo tras 4.5 segundos)
    LaunchedEffect(activeErrorMessage) {
        if (activeErrorMessage != null) {
            delay(4500)
            activeErrorMessage = null
        }
    }

    // Manejo de permisos para guardar en la libreta nativa del telefono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            syncToPhone = true
            activeErrorMessage = null
        } else {
            syncToPhone = false
            activeErrorMessage = "Se requiere permiso de contactos para guardar en el teléfono."
        }
    }

    // Sincronizar notificaciones emitidas por el ViewModel
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            activeErrorMessage = msg
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            activeErrorMessage = null
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    // Reglas de formato básico
    val isFirstNameValid = firstName.trim().length >= 2
    val isLastNameValid = lastName.trim().length >= 2
    val isPhoneValid = phone.trim().length == 9 && phone.trim().all { it.isDigit() }
    val isUsernameValid = username.isBlank() || username.trim().removePrefix("@").length >= 3
    val isEmailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isFormatValid = isFirstNameValid && isLastNameValid && isPhoneValid && isUsernameValid && isEmailValid

    // Filtrado reactivo en tiempo real
    val filteredContacts = remember(uiState.contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.contacts
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.contacts.filter { c ->
                c.fullName.lowercase().contains(q) ||
                (c.username?.lowercase()?.contains(q) == true) ||
                c.phone.contains(q)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agenda de Contactos",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.contacts.size} registrados",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al menú principal"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadContacts() },
                        enabled = !uiState.loading
                    ) {
                        if (uiState.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Recargar contactos")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ============================================================
            // CONTENIDO PRINCIPAL: Formulario y Lista
            // ============================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECCION 1: Formulario de Registro
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Nuevo Contacto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // 1. Fila de Nombre
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = {
                                        firstName = it
                                        activeErrorMessage = null
                                    },
                                    label = { Text("Nombre") },
                                    placeholder = { Text("Ej. Juan") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    isError = attemptedSubmit && !isFirstNameValid,
                                    supportingText = if (attemptedSubmit && !isFirstNameValid) {
                                        { Text("Mínimo 2 caracteres", color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 2. Fila de Apellido
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(Modifier.width(40.dp))
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = {
                                        lastName = it
                                        activeErrorMessage = null
                                    },
                                    label = { Text("Apellido") },
                                    placeholder = { Text("Ej. Pérez") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    isError = attemptedSubmit && !isLastNameValid,
                                    supportingText = if (attemptedSubmit && !isLastNameValid) {
                                        { Text("Mínimo 2 caracteres", color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 3. Fila de Nombre de Usuario (@ Opcional)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = {
                                        username = it
                                        activeErrorMessage = null
                                    },
                                    label = { Text("Nombre de usuario (opcional)") },
                                    placeholder = { Text("Ej. juanperez") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    isError = attemptedSubmit && !isUsernameValid,
                                    supportingText = if (attemptedSubmit && !isUsernameValid) {
                                        { Text("Mínimo 3 caracteres si se ingresa", color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 4. Fila de Telefono con Selector de Pais
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(14.dp))

                                // Selector de Pais con Dropdown
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        border = CardDefaults.outlinedCardBorder(),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .height(56.dp)
                                            .clickable { countryDropdownExpanded = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = "${selectedCountry.code} ${selectedCountry.dialCode}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Elegir país",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = countryDropdownExpanded,
                                        onDismissRequest = { countryDropdownExpanded = false }
                                    ) {
                                        COUNTRY_OPTIONS.forEach { country ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${country.flag} ${country.name} (${country.dialCode})")
                                                },
                                                onClick = {
                                                    selectedCountry = country
                                                    countryDropdownExpanded = false
                                                    activeErrorMessage = null
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 9) {
                                            phone = digits
                                            activeErrorMessage = null
                                        }
                                    },
                                    label = { Text("Teléfono") },
                                    placeholder = { Text("987654321") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    isError = attemptedSubmit && !isPhoneValid,
                                    supportingText = if (attemptedSubmit && !isPhoneValid) {
                                        { Text("Exactamente 9 dígitos", color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 5. Fila de Correo Electronico (Opcional)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        activeErrorMessage = null
                                    },
                                    label = { Text("Correo (opcional)") },
                                    placeholder = { Text("contacto@gmail.com") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(12.dp),
                                    isError = attemptedSubmit && !isEmailValid,
                                    supportingText = if (attemptedSubmit && !isEmailValid) {
                                        { Text("Correo no válido", color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 6. Switch: Guardar en el telefono
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Guardar en el teléfono",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Guarda el contacto en la libreta física de tu móvil",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = syncToPhone,
                                    onCheckedChange = { desiredState ->
                                        if (desiredState) {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.WRITE_CONTACTS
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (hasPermission) {
                                                syncToPhone = true
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                                            }
                                        } else {
                                            syncToPhone = false
                                        }
                                    }
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            // Boton para Guardar Contacto
                            Button(
                                onClick = {
                                    attemptedSubmit = true

                                    // Validacion 1: Formato basico
                                    if (!isFormatValid) {
                                        activeErrorMessage = "Por favor complete los campos obligatorios requeridos."
                                        return@Button
                                    }

                                    val cleanFirst = firstName.trim()
                                    val cleanLast = lastName.trim()
                                    val cleanPhone = phone.trim()
                                    val cleanCountry = selectedCountry.dialCode
                                    val cleanUser = username.trim().removePrefix("@").takeIf { it.isNotBlank() }

                                    // Validacion 2: Insensible a mayusculas/minusculas contra los contactos existentes
                                    val currentContacts = uiState.contacts
                                    val existingByName = currentContacts.firstOrNull {
                                        it.firstName.trim().equals(cleanFirst, ignoreCase = true) &&
                                        it.lastName.trim().equals(cleanLast, ignoreCase = true)
                                    }

                                    if (existingByName != null) {
                                        if (existingByName.phone.trim() == cleanPhone) {
                                            activeErrorMessage = "Ya existe un contacto registrado con este mismo nombre y número de teléfono."
                                            return@Button
                                        } else {
                                            activeErrorMessage = "Ya existe un contacto con el nombre \"${existingByName.fullName}\". Por favor, utilice otro nombre o agregue un distintivo."
                                            return@Button
                                        }
                                    }

                                    // Validacion 3: Mismo numero telefonico
                                    val existingByPhone = currentContacts.firstOrNull {
                                        it.phone.trim() == cleanPhone && it.countryCode.trim() == cleanCountry
                                    }
                                    if (existingByPhone != null) {
                                        activeErrorMessage = "El número $cleanCountry $cleanPhone ya pertenece a \"${existingByPhone.fullName}\"."
                                        return@Button
                                    }

                                    // Validacion 4: Mismo nombre de usuario
                                    if (cleanUser != null) {
                                        val existingByUser = currentContacts.firstOrNull {
                                            it.username?.trim()?.removePrefix("@")?.equals(cleanUser, ignoreCase = true) == true
                                        }
                                        if (existingByUser != null) {
                                            activeErrorMessage = "El nombre de usuario \"@$cleanUser\" ya está registrado. Por favor elija otro."
                                            return@Button
                                        }
                                    }

                                    // Si pasa todas las validaciones:
                                    activeErrorMessage = null

                                    val currentFirst = firstName
                                    val currentLast = lastName
                                    val currentUsername = username
                                    val currentPhone = phone
                                    val currentEmail = email
                                    val currentSync = syncToPhone

                                    // Guardar en el dispositivo fisico si se marco el interruptor
                                    if (currentSync) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.WRITE_CONTACTS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            DeviceContactsUtil.saveContactToDevice(
                                                context = context,
                                                firstName = currentFirst,
                                                lastName = currentLast,
                                                phone = "$cleanCountry $currentPhone",
                                                email = currentEmail
                                            )
                                        }
                                    }

                                    // Limpieza inmediata del formulario y ocultar teclado
                                    firstName = ""
                                    lastName = ""
                                    username = ""
                                    phone = ""
                                    email = ""
                                    syncToPhone = false
                                    attemptedSubmit = false
                                    keyboardController?.hide()

                                    viewModel.addContact(
                                        firstName = currentFirst,
                                        lastName = currentLast,
                                        username = currentUsername,
                                        countryCode = selectedCountry.dialCode,
                                        phone = currentPhone,
                                        email = currentEmail,
                                        syncToPhone = currentSync,
                                        onError = { errorMsg ->
                                            firstName = currentFirst
                                            lastName = currentLast
                                            username = currentUsername
                                            phone = currentPhone
                                            email = currentEmail
                                            syncToPhone = currentSync
                                            attemptedSubmit = true
                                            activeErrorMessage = errorMsg
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .pressScale()
                            ) {
                                if (uiState.saving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Guardando...")
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Guardar Contacto", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // SECCION 2: Barra de Busqueda
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        },
                        placeholder = { Text("Buscar por nombre, usuario o teléfono...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SECCION 3: Listado de Contactos
                if (uiState.loading && uiState.contacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredContacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No hay contactos registrados"
                                           else "Sin resultados para \"$searchQuery\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(filteredContacts, key = { _, c -> c.id }) { _, contact ->
                        ContactCardItem(
                            contact = contact,
                            onDelete = { contactToDelete = contact }
                        )
                    }
                }
            }

            // ============================================================
            // NOTIFICACION FLOTANTE ESTILO APPLE (DYNAMIC BANNER)
            // ============================================================
            AnimatedVisibility(
                visible = activeErrorMessage != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .zIndex(100f),
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { -it } + fadeIn(tween(180)),
                exit = slideOutVertically(
                    animationSpec = tween(220)
                ) { -it } + fadeOut(tween(200))
            ) {
                activeErrorMessage?.let { message ->
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFD32F2F),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aviso de Validación",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = message,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            IconButton(
                                onClick = { activeErrorMessage = null },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de confirmacion para eliminar contacto
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Eliminar contacto") },
            text = {
                Text("¿Estás seguro de que deseas eliminar a \"${contact.fullName}\"? Esta acción no se puede deshacer.")
            },
            shape = RoundedCornerShape(20.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContact(contact.id)
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
 * Tarjeta individual para mostrar un contacto en la lista.
 */
@Composable
private fun ContactCardItem(
    contact: Contact,
    onDelete: () -> Unit
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
            // Avatar con iniciales
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(14.dp))

            // Informacion del contacto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Nombre de usuario @
                contact.displayUsername?.let { u ->
                    Text(
                        text = u,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                }

                // Telefono
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = contact.fullPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Correo si existe
                contact.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badge de sincronizacion con el telefono si aplica
                if (contact.syncToPhone) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "En el teléfono",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Boton para eliminar
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar contacto",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        }
    }
}
