package com.unp.emprendepiura.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unp.emprendepiura.core.theme.applePopEnter
import com.unp.emprendepiura.core.theme.applePopExit
import com.unp.emprendepiura.core.theme.applePushEnter
import com.unp.emprendepiura.core.theme.applePushExit
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.features.auth.ui.BiometricAuthScreen
import com.unp.emprendepiura.features.auth.ui.BiometricViewModel
import com.unp.emprendepiura.features.auth.ui.BiometricVista
import com.unp.emprendepiura.features.auth.ui.components.FingerprintAuthModal
import com.unp.emprendepiura.features.auth.ui.components.LiveFaceScannerModal
import com.unp.emprendepiura.features.auth.ui.components.SwipeableFeedbackBanner
import com.unp.emprendepiura.features.marketplace.MarketplaceShellScreen
import com.unp.emprendepiura.features.splash.SplashScreen

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_AUTH = "auth"
private const val ROUTE_MARKETPLACE = "marketplace"

@Composable
fun AppNav(
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val nav: NavHostController = rememberNavController()
    val biometricVm: BiometricViewModel = viewModel()
    val authState by biometricVm.state.collectAsState()
    val context = LocalContext.current

    var abrirFacialModoRegistro by remember { mutableStateOf(false) }
    var authInicioEnRegistro by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            biometricVm.mostrarModalEscanerFacial(esModoRegistro = abrirFacialModoRegistro)
        } else {
            biometricVm.mostrarError("Se requiere permiso de cámara para usar el escáner facial en vivo.")
        }
    }

    fun abrirEscanerFacialConPermiso(esModoRegistro: Boolean) {
        abrirFacialModoRegistro = esModoRegistro
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                biometricVm.mostrarModalEscanerFacial(esModoRegistro = esModoRegistro)
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun salirDeAuthHaciaMarketplace() {
        authInicioEnRegistro = false
        if (nav.currentBackStackEntry?.destination?.route == ROUTE_AUTH) {
            if (!nav.popBackStack(ROUTE_MARKETPLACE, inclusive = false)) {
                nav.navigate(ROUTE_MARKETPLACE) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = ROUTE_SPLASH,
            enterTransition = {
                if (initialState.destination.route == ROUTE_SPLASH) {
                    fadeIn(tween(340)) + scaleIn(initialScale = 0.985f, animationSpec = applePushSpring())
                } else {
                    applePushEnter()
                }
            },
            exitTransition = {
                if (initialState.destination.route == ROUTE_SPLASH) {
                    fadeOut(tween(240))
                } else {
                    applePushExit()
                }
            },
            popEnterTransition = { applePopEnter() },
            popExitTransition = { applePopExit() }
        ) {
            composable(ROUTE_SPLASH) {
                SplashScreen(
                    onFinished = {
                        nav.navigate(ROUTE_MARKETPLACE) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_AUTH) {
                BiometricAuthScreen(
                    viewModel = biometricVm,
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    volverDirectoDesdeRegistro = authInicioEnRegistro,
                    onSolicitarCamaraFacial = ::abrirEscanerFacialConPermiso,
                    onEntrarAlMarketplace = ::salirDeAuthHaciaMarketplace,
                    onVolverAlMarketplace = {
                        biometricVm.limpiarFormulariosAuth(context)
                        salirDeAuthHaciaMarketplace()
                    }
                )
            }

            composable(ROUTE_MARKETPLACE) {
                MarketplaceShellScreen(
                    usuario = authState.usuarioAutenticado,
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    onIrAAuth = {
                        authInicioEnRegistro = false
                        biometricVm.prepararLoginLimpio(context)
                        nav.navigate(ROUTE_AUTH) {
                            launchSingleTop = true
                        }
                    },
                    onIrARegistro = {
                        authInicioEnRegistro = true
                        biometricVm.irARegistro(context)
                        nav.navigate(ROUTE_AUTH) {
                            launchSingleTop = true
                        }
                    },
                    onEditarPerfilBiometria = {
                        authInicioEnRegistro = true
                        biometricVm.irAEditarBiometria(context)
                        nav.navigate(ROUTE_AUTH) {
                            launchSingleTop = true
                        }
                    },
                    onRecapturarHuella = biometricVm::iniciarCambioHuellaEnSesion,
                    onRecapturarRostro = {
                        biometricVm.iniciarRecapturaRostroEnSesion {
                            abrirEscanerFacialConPermiso(true)
                        }
                    },
                    onRefrescarUsuario = { biometricVm.refrescarUsuarioAutenticado(context) },
                    onCerrarSesion = {
                        biometricVm.cerrarSesion()
                    }
                )
            }
        }

        // Modales biometricos globales reutilizables (tanto en Auth como en recaptura desde Perfil)
        FingerprintAuthModal(
            visible = authState.mostrarModalHuella,
            esModoRegistro = authState.huellaEnModoRegistro,
            dniObjetivo = authState.huellaDniObjetivo,
            subtituloUsuario = authState.huellaSubtitulo,
            errorInicial = authState.huellaErrorInicial,
            onHuellaVerificada = { token -> biometricVm.confirmarHuellaVerificada(context, token) },
            onCancelar = biometricVm::cerrarModalHuella
        )

        LiveFaceScannerModal(
            visible = authState.mostrarEscanerFacial,
            esModoRegistro = authState.escanerEnModoRegistro,
            subtituloTitular = authState.facialSubtituloTitular,
            onCompletarEscaneo = { vectorPromedio, frameBitmap ->
                biometricVm.evaluarEscaneoFacialEnVivo(context, vectorPromedio, frameBitmap)
            },
            onFinalizarConExito = { biometricVm.finalizarEscaneoFacialExitoso(context) },
            onCancelar = biometricVm::cerrarEscanerFacial
        )

        SwipeableFeedbackBanner(
            errorMessage = authState.errorMessage,
            successMessage = authState.successMessage,
            onClearMessages = biometricVm::clearMessages,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
