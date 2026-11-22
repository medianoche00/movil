package com.unp.emprendepiura.features.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.unp.emprendepiura.core.theme.appleSubRouteTransition

/*
 * Archivo: BiometricAuthScreen.kt
 * Proposito: Apartado dedicado exclusivamente al Inicio de Sesion (DNI + Huella / Rostro / Contrasena)
 *            y al Registro Biometrico de usuarios de EmprendePiura.
 */
@Composable
fun BiometricAuthScreen(
    viewModel: BiometricViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    volverDirectoDesdeRegistro: Boolean = false,
    onSolicitarCamaraFacial: (esModoRegistro: Boolean) -> Unit,
    onEntrarAlMarketplace: () -> Unit,
    onVolverAlMarketplace: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.limpiarFormulariosAuth(context)
        }
    }

    LaunchedEffect(state.vistaActual) {
        if (state.vistaActual == BiometricVista.MARKETPLACE) {
            onEntrarAlMarketplace()
        }
    }

    BackHandler {
        when {
            state.mostrarCampoContrasena -> viewModel.ocultarContrasenaInline()
            state.vistaActual == BiometricVista.REGISTRO -> {
                if (volverDirectoDesdeRegistro) {
                    onVolverAlMarketplace()
                } else {
                    viewModel.volverABienvenida()
                }
            }
            else -> onVolverAlMarketplace()
        }
    }

    val esVistaRegistro = state.vistaActual == BiometricVista.REGISTRO

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = esVistaRegistro,
            transitionSpec = { appleSubRouteTransition(esRegresoATabs = !targetState) },
            label = "authSubRoute"
        ) { enRegistro ->
            if (!enRegistro) {
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
                            onSolicitarCamaraFacial(false)
                        }
                    },
                    onMostrarContrasenaClick = { viewModel.desplegarContrasenaInline(context) },
                    onOcultarContrasenaClick = viewModel::ocultarContrasenaInline,
                    onConfirmarContrasena = { viewModel.loginConContrasena(context) },
                    onIrARegistro = { viewModel.irARegistro(context) },
                    onVolver = onVolverAlMarketplace,
                    onToggleTheme = onToggleTheme
                )
            } else {
                RegistroBiometricoScreen(
                    dni = state.regDni,
                    nombre = state.regNombre,
                    telefono = state.regTelefono,
                    correo = state.regCorreo,
                    zonaPiura = state.regZonaPiura,
                    contrasena = state.regContrasena,
                    huellaVinculada = state.huellaRegistroLista,
                    fotoFacialUri = state.regFotoFacialUri,
                    rostroVerificado = state.rostroRegistroListo,
                    analizandoRostro = state.procesandoRostro,
                    saving = state.saving,
                    esModoEdicion = state.esModoEdicion,
                    buscandoUsuario = state.buscandoUsuarioDni,
                    dniError = state.regDniError,
                    onDniChange = { dni -> viewModel.onRegDniChange(dni, context) },
                    onNombreChange = viewModel::onRegNombreChange,
                    onTelefonoChange = viewModel::onRegTelefonoChange,
                    onCorreoChange = viewModel::onRegCorreoChange,
                    onZonaChange = viewModel::onRegZonaChange,
                    onContrasenaChange = viewModel::onRegContrasenaChange,
                    onVincularHuella = viewModel::abrirModalHuellaRegistro,
                    onCapturarRostro = {
                        viewModel.prepararRegistroFacial {
                            onSolicitarCamaraFacial(true)
                        }
                    },
                    onGuardarUsuario = { viewModel.guardarUsuario(context) },
                    onVolver = {
                        if (volverDirectoDesdeRegistro) {
                            onVolverAlMarketplace()
                        } else {
                            viewModel.volverABienvenida()
                        }
                    }
                )
            }
        }
    }
}
