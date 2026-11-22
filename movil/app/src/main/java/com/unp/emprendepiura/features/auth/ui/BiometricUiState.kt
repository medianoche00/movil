package com.unp.emprendepiura.features.auth.ui

import android.net.Uri
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Usuario

enum class BiometricVista {
    BIENVENIDA,
    REGISTRO,
    MARKETPLACE
}

data class BiometricUiState(
    val vistaActual: BiometricVista = BiometricVista.BIENVENIDA,

    // Campos de Bienvenida (DNI primero + Contrasena desplegable inline)
    val loginDni: String = "",
    val loginContrasena: String = "",
    val mostrarCampoContrasena: Boolean = false,
    val dniInlineError: String? = null,
    val dniShakeTrigger: Int = 0,

    // Campos de Registro y Edicion unificados con EmprendePiura
    val regDni: String = "",
    val regNombre: String = "",
    val regTelefono: String = "",
    val regCorreo: String = "",
    val regZonaPiura: String = PiuraConstants.ZONAS_PIURA.first(),
    val regContrasena: String = "",
    val regPasswordHashActual: String = "",
    val regHuellaToken: String = "",
    val regFotoFacialUri: Uri? = null,
    val regFotoFacialBytes: ByteArray? = null,
    val regVectorFacial: String = "",
    val esModoEdicion: Boolean = false,
    val buscandoUsuarioDni: Boolean = false,
    val regDniError: String? = null,

    // Modal de huella dactilar
    val mostrarModalHuella: Boolean = false,
    val huellaEnModoRegistro: Boolean = false,
    val huellaDniObjetivo: String = "",
    val huellaSubtitulo: String = "",
    val huellaErrorInicial: String? = null,
    val huellaUsuarioObjetivo: Usuario? = null,

    // Modal de escaner facial en vivo 1:1
    val mostrarEscanerFacial: Boolean = false,
    val escanerEnModoRegistro: Boolean = false,
    val facialSubtituloTitular: String = "",
    val procesandoRostro: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,

    // Sesion autenticada en EmprendePiura
    val usuarioAutenticado: Usuario? = null,
    val metodoAutenticacion: String = "",

    // Banner deslizable
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val huellaRegistroLista: Boolean
        get() = regHuellaToken.isNotBlank()

    val rostroRegistroListo: Boolean
        get() = (regFotoFacialBytes != null || regFotoFacialUri != null) && regVectorFacial.isNotBlank()
}
