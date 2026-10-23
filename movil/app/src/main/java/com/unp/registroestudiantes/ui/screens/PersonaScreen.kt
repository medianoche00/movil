package com.unp.registroestudiantes.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Profesion
import com.unp.registroestudiantes.viewmodel.PersonaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Archivo: PersonaScreen.kt
 * Proposito: Pantalla principal del Trabajo N°03 - Registro de Personas.
 *
 * Funcionalidades implementadas:
 *   [Buscar]    - Consulta por DNI (llave primaria)
 *   [Nuevo]     - Limpia el formulario para un nuevo registro
 *   [Guardar]   - Inserta una persona nueva con su foto
 *   [Actualizar]- Modifica los datos de una persona existente
 *   [Eliminar]  - Borra el registro de la persona
 *   [Foto]      - Abre la cámara para capturar la fotografía
 *
 * Arquitectura:
 *   PersonaScreen (Vista) -> PersonaViewModel (Estado) -> PersonaRepository (Datos)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    viewModel: PersonaViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // ----------------------------------------------------------------
    // Gestión de permisos y lanzador de cámara
    // ----------------------------------------------------------------

    // URI temporal donde se guarda la foto tomada
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Lanzador de la cámara (ActivityResultContracts.TakePicture)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                viewModel.onFotoCapturada(uri)
            }
        }
    }

    // Lanzador de solicitud de permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = crearArchivoFotoTemporal(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Función para disparar la cámara respetando los permisos
    fun abrirCamara() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                val uri = crearArchivoFotoTemporal(context)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Estado de diálogo de confirmación para ELIMINAR
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    // ----------------------------------------------------------------
    // Layout principal
    // ----------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Registro de Personas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Trabajo N°03",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al menú")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->

        // Diálogo de confirmación para eliminar
        if (mostrarDialogoEliminar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoEliminar = false },
                icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Confirmar eliminación", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "¿Está seguro de que desea eliminar a \"${state.personaActual?.nombreCompleto}\" " +
                        "con DNI ${state.personaActual?.dni}?\n\nEsta acción no se puede deshacer."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogoEliminar = false
                            viewModel.eliminar()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { mostrarDialogoEliminar = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --------------------------------------------------------
            // Seccion 1: Foto + Búsqueda por DNI
            // --------------------------------------------------------
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar / Foto
                    FotoPersonaWidget(
                        fotoUri = state.fotoUri,
                        fotoUrl = state.fotoPath,
                        iniciales = if (state.personaActual != null) state.personaActual!!.iniciales
                                    else if (state.nombres.isNotBlank() || state.apellidos.isNotBlank())
                                        "${state.nombres.trim().firstOrNull()?.uppercaseChar() ?: ""}${state.apellidos.trim().firstOrNull()?.uppercaseChar() ?: ""}"
                                    else "?",
                        onFotoClick = { abrirCamara() }
                    )

                    Text(
                        text = "Toca la foto para capturar con la cámara",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider()

                    // Campo DNI + botón buscar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.dni,
                            onValueChange = viewModel::onDniChange,
                            label = { Text("DNI *") },
                            leadingIcon = { Icon(Icons.Default.Badge, null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Search
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            supportingText = { Text("8 dígitos – llave primaria") }
                        )
                        FilledTonalButton(
                            onClick = { viewModel.buscarPorDni(state.dni) },
                            enabled = !state.loading && state.dni.length == 8,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, "Buscar")
                                Spacer(Modifier.width(4.dp))
                                Text("Buscar")
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------------
            // Seccion 2: Formulario de datos
            // --------------------------------------------------------
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Person,
                        title = "Datos Personales"
                    )

                    // Nombres
                    OutlinedTextField(
                        value = state.nombres,
                        onValueChange = viewModel::onNombresChange,
                        label = { Text("Nombres *") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Apellidos
                    OutlinedTextField(
                        value = state.apellidos,
                        onValueChange = viewModel::onApellidosChange,
                        label = { Text("Apellidos *") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dirección
                    OutlinedTextField(
                        value = state.direccion,
                        onValueChange = viewModel::onDireccionChange,
                        label = { Text("Dirección *") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Celular
                    OutlinedTextField(
                        value = state.celular,
                        onValueChange = viewModel::onCelularChange,
                        label = { Text("Celular *") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        supportingText = { Text("9 dígitos") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Correo electrónico
                    OutlinedTextField(
                        value = state.correoElectronico,
                        onValueChange = viewModel::onCorreoChange,
                        label = { Text("Correo Electrónico *") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Profesión (DropdownMenu tipo Spinner)
                    SectionHeader(
                        icon = Icons.Default.Work,
                        title = "Profesión"
                    )

                    if (state.loadingProfesiones) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Cargando profesiones...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        ProfesionDropdown(
                            profesiones = state.profesiones,
                            selectedCodigo = state.codigoProfesion,
                            onSelect = viewModel::onProfesionChange
                        )
                    }
                }
            }

            // --------------------------------------------------------
            // Seccion 3: Barra de acciones CRUD
            // --------------------------------------------------------
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Settings,
                        title = "Acciones"
                    )

                    // Fila 1: Nuevo + Guardar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.nuevo() },
                            modifier = Modifier.weight(1f),
                            enabled = !state.saving
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nuevo")
                        }

                        Button(
                            onClick = { viewModel.guardar(context) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.saving && !state.modoEdicion
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Guardar")
                            }
                        }
                    }

                    // Fila 2: Actualizar + Eliminar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.actualizar(context) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.saving && state.modoEdicion
                        ) {
                            if (state.saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Actualizar")
                            }
                        }

                        Button(
                            onClick = { mostrarDialogoEliminar = true },
                            modifier = Modifier.weight(1f),
                            enabled = !state.saving && state.modoEdicion,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    }

                    // Botón Foto (centrado, más prominente)
                    Button(
                        onClick = { abrirCamara() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Capturar Fotografía", fontWeight = FontWeight.SemiBold)
                    }

                    // Indicador del modo actual
                    ModoChip(modoEdicion = state.modoEdicion)
                }
            }

            // --------------------------------------------------------
            // Seccion 4: Mensajes de feedback
            // --------------------------------------------------------
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                state.errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { viewModel.clearMessages() }) {
                                Icon(
                                    Icons.Default.Close,
                                    "Cerrar",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.successMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                state.successMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { viewModel.clearMessages() }) {
                                Icon(
                                    Icons.Default.Close,
                                    "Cerrar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================
// Componentes auxiliares
// ============================================================

/**
 * Avatar circular que muestra la foto capturada (URI local), la URL de Supabase,
 * o las iniciales del nombre si no hay foto disponible.
 * Al hacer clic se dispara la cámara.
 */
@Composable
private fun FotoPersonaWidget(
    fotoUri: Uri?,
    fotoUrl: String?,
    iniciales: String,
    onFotoClick: () -> Unit
) {
    val imagenMostrar: Any? = fotoUri ?: fotoUrl?.takeIf { it.isNotBlank() }

    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6750A4),
                        Color(0xFF9C27B0)
                    )
                )
            )
            .border(3.dp, Color(0xFF6750A4).copy(alpha = 0.5f), CircleShape)
            .clickable { onFotoClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imagenMostrar != null) {
            AsyncImage(
                model = imagenMostrar,
                contentDescription = "Foto de la persona",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = iniciales,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Badge de cámara en la esquina inferior-derecha
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "Tomar foto",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * DropdownMenu de tipo Spinner para seleccionar la profesión.
 */
@Composable
private fun ProfesionDropdown(
    profesiones: List<Profesion>,
    selectedCodigo: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = profesiones.firstOrNull { it.codigoProfesion == selectedCodigo }?.nombre
        ?: "Seleccione una profesión"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Profesión *") },
            leadingIcon = { Icon(Icons.Default.Work, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            profesiones.forEach { prof ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(prof.nombre, fontWeight = FontWeight.Medium)
                            Text(
                                prof.codigoProfesion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(prof.codigoProfesion)
                        expanded = false
                    },
                    leadingIcon = if (prof.codigoProfesion == selectedCodigo) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

/**
 * Encabezado visual de sección con icono y título.
 */
@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Chip indicador del modo actual (NUEVO o EDICIÓN).
 */
@Composable
private fun ModoChip(modoEdicion: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (modoEdicion)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (modoEdicion) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (modoEdicion)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (modoEdicion) "Modo Edición" else "Modo Nuevo Registro",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (modoEdicion)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================================
// Función utilitaria: crea el archivo temporal para la foto
// ============================================================

/**
 * Crea un archivo JPEG vacío en el directorio de imágenes privado de la app
 * y devuelve su URI segura via FileProvider.
 *
 * La URI se pasa al Intent de la cámara para que guarde la foto ahí directamente.
 * Se usa el timestamp como nombre para evitar colisiones entre capturas.
 */
private fun crearArchivoFotoTemporal(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile("PERSONA_${timestamp}_", ".jpg", storageDir)

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}
