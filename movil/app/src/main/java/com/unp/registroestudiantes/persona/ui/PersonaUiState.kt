package com.unp.registroestudiantes.persona.ui

import android.net.Uri
import com.unp.registroestudiantes.persona.data.model.Persona
import com.unp.registroestudiantes.persona.data.model.Profesion

/*
 * Archivo: PersonaUiState.kt
 * Proposito: Estado inmutable de la interfaz de usuario para el modulo de Personas.
 */

enum class PersonaVista {
    FORMULARIO,
    DIRECTORIO,
    DETALLE
}

data class PersonaUiState(
    // Catalogo
    val profesiones: List<Profesion> = emptyList(),
    val loadingProfesiones: Boolean = false,

    // Formulario
    val dni: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val direccion: String = "",
    val codigoProfesion: String = "",
    val celular: String = "",
    val correoElectronico: String = "",
    val fotoUri: Uri? = null,
    val fotoPath: String? = null,

    // Modos de operacion y navegacion interna
    val personaActual: Persona? = null,
    val vistaActual: PersonaVista = PersonaVista.FORMULARIO,
    val personaSeleccionadaDetalle: Persona? = null,
    val directorio: List<Persona> = emptyList(),
    val loadingDirectorio: Boolean = false,

    val loading: Boolean = false,
    val saving: Boolean = false,

    // Mensajes de notificacion
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    /** Determina si se esta editando un registro existente cargado en pantalla. */
    val modoEdicion: Boolean
        get() = personaActual != null
}
