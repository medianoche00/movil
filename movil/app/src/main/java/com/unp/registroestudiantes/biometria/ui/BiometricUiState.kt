package com.unp.registroestudiantes.biometria.ui

import android.net.Uri
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometrico

/*
 * Archivo: BiometricUiState.kt
 * Proposito: Estado inmutable de la interfaz para el modulo de Control Biometrico (Trabajo N°04)
 *            con DNI en pantalla principal y despliegue inline de contrasena.
 */

enum class BiometricVista {
    BIENVENIDA,
    REGISTRO,
    POST_LOGIN
}

data class BiometricUiState(
    val vistaActual: BiometricVista = BiometricVista.BIENVENIDA,

    // Campos de Bienvenida (DNI primero + Contrasena desplegable inline)
    val loginDni: String = "",
    val loginContrasena: String = "",
    val mostrarCampoContrasena: Boolean = false,
    val dniInlineError: String? = null,
    val dniShakeTrigger: Int = 0,

    // Campos de Registro de Usuario
    val regDni: String = "",
    val regNombre: String = "",
    val regContrasena: String = "",
    val regHuellaToken: String = "",
    val regFotoFacialUri: Uri? = null,
    val regFotoFacialBytes: ByteArray? = null,
    val regVectorFacial: String = "",

    // Control del modal de huella dactilar (errores dentro del modal)
    val mostrarModalHuella: Boolean = false,
    val huellaEnModoRegistro: Boolean = false,
    val huellaDniObjetivo: String = "",
    val huellaSubtitulo: String = "",
    val huellaErrorInicial: String? = null,
    val huellaUsuarioObjetivo: UsuarioBiometrico? = null,

    // Control del escaner facial en vivo 1:1 con CameraX
    val mostrarEscanerFacial: Boolean = false,
    val escanerEnModoRegistro: Boolean = false,
    val facialSubtituloTitular: String = "",
    val procesandoRostro: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,

    // Sesion autenticada (Pantalla Tema · En Construccion)
    val usuarioAutenticado: UsuarioBiometrico? = null,
    val metodoAutenticacion: String = "",

    // Mensajes de notificacion superior deslizable
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val huellaRegistroLista: Boolean
        get() = regHuellaToken.isNotBlank()

    val rostroRegistroListo: Boolean
        get() = regFotoFacialUri != null && regFotoFacialBytes != null && regVectorFacial.isNotBlank()
}
