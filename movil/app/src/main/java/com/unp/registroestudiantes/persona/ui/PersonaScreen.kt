package com.unp.registroestudiantes.persona.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.unp.registroestudiantes.persona.ui.components.*
import com.unp.registroestudiantes.persona.util.CameraUtil

/*
 * Archivo: PersonaScreen.kt
 * Proposito: Pantalla principal de Personas con navegacion interna fluida entre:
 *            - Formulario de Registro / Edición
 *            - Directorio de Personas Registradas
 *            - Ficha de Detalle de Persona
 *            - Visor de Fotografía a Pantalla Completa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    viewModel: PersonaViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var fotoZoomPersona by remember { mutableStateOf<Pair<Any, String>?>(null) }

    // Manejo de retroceso del sistema
    BackHandler {
        when (state.vistaActual) {
            PersonaVista.DETALLE -> viewModel.abrirDirectorio()
            PersonaVista.DIRECTORIO -> viewModel.volverAlFormulario()
            PersonaVista.FORMULARIO -> onBack()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri -> viewModel.onFotoCapturada(uri) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = CameraUtil.crearArchivoFotoTemporal(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun abrirCamara() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                val uri = CameraUtil.crearArchivoFotoTemporal(context)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Visor de foto en pantalla completa cuando se solicita
    if (fotoZoomPersona != null) {
        PersonaFullScreenPhotoDialog(
            fotoModel = fotoZoomPersona!!.first,
            nombre = fotoZoomPersona!!.second,
            dni = if (state.personaActual != null) state.personaActual!!.dni else state.dni,
            onDismiss = { fotoZoomPersona = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.vistaActual) {
            PersonaVista.DIRECTORIO -> {
                PersonaDirectoryScreen(
                    directorio = state.directorio,
                    loading = state.loadingDirectorio,
                    profesiones = state.profesiones,
                    onBack = viewModel::volverAlFormulario,
                    onPersonaClick = { persona -> viewModel.abrirDetalle(persona) },
                    onNuevo = {
                        viewModel.nuevo()
                        viewModel.volverAlFormulario()
                    }
                )
            }

            PersonaVista.DETALLE -> {
                PersonaDetailScreen(
                    persona = state.personaSeleccionadaDetalle,
                    profesiones = state.profesiones,
                    onBack = viewModel::abrirDirectorio,
                    onModificar = { persona -> viewModel.modificarDesdeDetalle(persona) },
                    onVerFotoCompleta = { foto, nombre ->
                        fotoZoomPersona = Pair(foto, nombre)
                    }
                )
            }

            PersonaVista.FORMULARIO -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Persona",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = "Trabajo N°03 · Gestión Académica",
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
                            actions = {
                                // Boton Directorio
                                TextButton(
                                    onClick = { viewModel.abrirDirectorio() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Directorio",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                ModoChip(
                                    modoEdicion = state.modoEdicion,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { padding ->
                    PersonaDeleteDialog(
                        persona = if (mostrarDialogoEliminar) state.personaActual else null,
                        onConfirm = {
                            mostrarDialogoEliminar = false
                            viewModel.eliminar()
                        },
                        onDismiss = { mostrarDialogoEliminar = false }
                    )

                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Campos del formulario
                                PersonaFormFields(
                                    dni = state.dni,
                                    nombres = state.nombres,
                                    apellidos = state.apellidos,
                                    direccion = state.direccion,
                                    codigoProfesion = state.codigoProfesion,
                                    celular = state.celular,
                                    correoElectronico = state.correoElectronico,
                                    profesiones = state.profesiones,
                                    loadingProfesiones = state.loadingProfesiones,
                                    loadingBuscar = state.loading,
                                    onDniChange = viewModel::onDniChange,
                                    onBuscar = { viewModel.buscarPorDni(state.dni) },
                                    onNombresChange = viewModel::onNombresChange,
                                    onApellidosChange = viewModel::onApellidosChange,
                                    onDireccionChange = viewModel::onDireccionChange,
                                    onProfesionSelect = viewModel::onProfesionChange,
                                    onCelularChange = viewModel::onCelularChange,
                                    onCorreoChange = viewModel::onCorreoChange
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )

                                // 2. Seccion Fotografica
                                val iniciales = when {
                                    state.personaActual != null -> state.personaActual!!.iniciales
                                    state.nombres.isNotBlank() || state.apellidos.isNotBlank() ->
                                        "${state.nombres.trim().firstOrNull()?.uppercaseChar() ?: ""}${state.apellidos.trim().firstOrNull()?.uppercaseChar() ?: ""}"
                                    else -> "?"
                                }

                                PersonaPhotoWidget(
                                    fotoUri = state.fotoUri,
                                    fotoUrl = state.fotoPath,
                                    iniciales = iniciales,
                                    onFotoClick = { abrirCamara() },
                                    onVerFotoCompleta = { foto ->
                                        val titulo = if (state.personaActual != null) state.personaActual!!.nombreCompleto
                                                     else "${state.nombres} ${state.apellidos}".trim().ifEmpty { "Fotografía" }
                                        fotoZoomPersona = Pair(foto, titulo)
                                    }
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )

                                // 3. Barra de acciones
                                PersonaActionButtons(
                                    saving = state.saving,
                                    modoEdicion = state.modoEdicion,
                                    onNuevo = viewModel::nuevo,
                                    onGuardar = { viewModel.guardar(context) },
                                    onActualizar = { viewModel.actualizar(context) },
                                    onEliminarClick = { mostrarDialogoEliminar = true }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        // Banner flotante superior permanente para avisos y notificaciones
        PersonaFeedbackBanners(
            errorMessage = state.errorMessage,
            successMessage = state.successMessage,
            onClearMessages = viewModel::clearMessages,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
