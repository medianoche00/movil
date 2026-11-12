package com.unp.registroestudiantes.biometria.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.unp.registroestudiantes.biometria.ui.components.FingerprintAuthModal
import com.unp.registroestudiantes.biometria.ui.components.LiveFaceScannerModal
import com.unp.registroestudiantes.biometria.ui.components.SwipeableFeedbackBanner

/*
 * Archivo: BiometricAuthScreen.kt
 * Proposito: Orquestador de vistas del Trabajo N°04 (Bienvenida con DNI primero y Contrasena
 *            inline, Registro Biometrico y Post-Login), modales de Huella y Rostro 1:1 en vivo,
 *            y banner superior deslizable.
 */
@Composable
fun BiometricAuthScreen(
    viewModel: BiometricViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onIrAPanelAcademico: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var abrirFacialEnModoRegistroPendiente by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.inicializarDniRecordado(context)
    }

    BackHandler {
        when {
            state.vistaActual == BiometricVista.REGISTRO -> viewModel.volverABienvenida()
            state.vistaActual == BiometricVista.POST_LOGIN -> viewModel.cerrarSesion()
            state.mostrarCampoContrasena -> viewModel.ocultarContrasenaInline()
            else -> (context as? Activity)?.finish()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.mostrarModalEscanerFacial(esModoRegistro = abrirFacialEnModoRegistroPendiente)
        } else {
            viewModel.mostrarError("Se requiere permiso de cámara para usar el escáner facial en vivo.")
        }
    }

    fun abrirCamaraConPermiso(esModoRegistro: Boolean) {
        abrirFacialEnModoRegistroPendiente = esModoRegistro
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                viewModel.mostrarModalEscanerFacial(esModoRegistro = esModoRegistro)
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 1. Modal de Huella Dactilar con manejo de errores integrado (Reintentar / Cancelar)
    FingerprintAuthModal(
        visible = state.mostrarModalHuella,
        esModoRegistro = state.huellaEnModoRegistro,
        dniObjetivo = state.huellaDniObjetivo,
        subtituloUsuario = state.huellaSubtitulo,
        errorInicial = state.huellaErrorInicial,
        onHuellaVerificada = { token -> viewModel.confirmarHuellaVerificada(context, token) },
        onCancelar = viewModel::cerrarModalHuella
    )

    // 2. Modal de Escaner Facial en Vivo 1:1 con CameraX (Marco Ambar -> Celeste -> Verde / Rojo)
    LiveFaceScannerModal(
        visible = state.mostrarEscanerFacial,
        esModoRegistro = state.escanerEnModoRegistro,
        subtituloTitular = state.facialSubtituloTitular,
        onCompletarEscaneo = { vectorPromedio, frameBitmap ->
            viewModel.evaluarEscaneoFacialEnVivo(context, vectorPromedio, frameBitmap)
        },
        onFinalizarConExito = viewModel::finalizarEscaneoFacialExitoso,
        onCancelar = viewModel::cerrarEscanerFacial
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.vistaActual) {
            BiometricVista.BIENVENIDA -> {
                WelcomeAuthScreen(
                    dni = state.loginDni,
                    contrasena = state.loginContrasena,
                    mostrarCampoContrasena = state.mostrarCampoContrasena,
                    dniInlineError = state.dniInlineError,
                    dniShakeTrigger = state.dniShakeTrigger,
                    loading = state.loading,
                    darkTheme = darkTheme,
                    onDniChange = viewModel::onLoginDniChange,
                    onContrasenaChange = viewModel::onLoginContrasenaChange,
                    onIngresarConHuella = { viewModel.abrirModalHuellaLogin(context) },
                    onIngresarConRostro = {
                        viewModel.prepararLoginFacial(context) {
                            abrirCamaraConPermiso(esModoRegistro = false)
                        }
                    },
                    onMostrarContrasenaClick = { viewModel.desplegarContrasenaInline(context) },
                    onOcultarContrasenaClick = viewModel::ocultarContrasenaInline,
                    onConfirmarContrasena = { viewModel.loginConContrasena(context) },
                    onIrARegistro = { viewModel.irARegistro(context) },
                    onToggleTheme = onToggleTheme
                )
            }

            BiometricVista.REGISTRO -> {
                RegistroBiometricoScreen(
                    dni = state.regDni,
                    nombre = state.regNombre,
                    contrasena = state.regContrasena,
                    huellaVinculada = state.huellaRegistroLista,
                    fotoFacialUri = state.regFotoFacialUri,
                    rostroVerificado = state.rostroRegistroListo,
                    analizandoRostro = state.procesandoRostro,
                    saving = state.saving,
                    esModoEdicion = state.esModoEdicion,
                    buscandoUsuario = state.buscandoUsuarioDni,
                    onDniChange = { dni -> viewModel.onRegDniChange(dni, context) },
                    onNombreChange = viewModel::onRegNombreChange,
                    onContrasenaChange = viewModel::onRegContrasenaChange,
                    onVincularHuella = viewModel::abrirModalHuellaRegistro,
                    onCapturarRostro = {
                        viewModel.prepararRegistroFacial {
                            abrirCamaraConPermiso(esModoRegistro = true)
                        }
                    },
                    onGuardarUsuario = { viewModel.guardarUsuario(context) },
                    onVolver = viewModel::volverABienvenida
                )
            }

            BiometricVista.POST_LOGIN -> {
                PostLoginScreen(
                    usuario = state.usuarioAutenticado,
                    metodoAutenticacion = state.metodoAutenticacion,
                    onIrAPanelAcademico = onIrAPanelAcademico,
                    onCerrarSesion = viewModel::cerrarSesion,
                    onEditarBiometria = { viewModel.irAEditarBiometria(context) },
                    onCambiarHuella = viewModel::iniciarCambioHuellaPostLogin
                )
            }
        }

        // Banner superior flotante deslizable hacia arriba y hacia los costados
        SwipeableFeedbackBanner(
            errorMessage = state.errorMessage,
            successMessage = state.successMessage,
            onClearMessages = viewModel::clearMessages,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
