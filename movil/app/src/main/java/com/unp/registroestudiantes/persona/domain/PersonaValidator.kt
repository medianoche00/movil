package com.unp.registroestudiantes.persona.domain

import android.util.Patterns
import com.unp.registroestudiantes.persona.data.model.Persona

/*
 * Archivo: PersonaValidator.kt
 * Proposito: Validaciones de negocio del formulario y mapeo de errores del sistema.
 */
object PersonaValidator {

    /** Valida el formato de busqueda por DNI (8 digitos numericos). */
    fun validarDni(dni: String): String? {
        val clean = dni.trim()
        return if (clean.length != 8 || !clean.all { it.isDigit() }) {
            "El DNI debe tener exactamente 8 dígitos numéricos."
        } else null
    }

    /** Valida los campos requeridos para guardar o actualizar una persona. */
    fun validarFormulario(
        dni: String,
        nombres: String,
        apellidos: String,
        direccion: String,
        codigoProfesion: String,
        celular: String,
        correoElectronico: String
    ): String? = when {
        dni.trim().length != 8 || !dni.trim().all { it.isDigit() } ->
            "El DNI debe tener exactamente 8 dígitos numéricos."
        nombres.isBlank() ->
            "El campo Nombres es obligatorio."
        apellidos.isBlank() ->
            "El campo Apellidos es obligatorio."
        direccion.isBlank() ->
            "El campo Dirección es obligatorio."
        codigoProfesion.isBlank() ->
            "Debe seleccionar una profesión."
        celular.trim().length != 9 || !celular.trim().all { it.isDigit() } ->
            "El celular debe tener exactamente 9 dígitos numéricos."
        correoElectronico.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(correoElectronico.trim()).matches() ->
            "Ingrese un correo electrónico válido."
        else -> null
    }

    private fun normalizarEspacios(texto: String): String =
        texto.trim().replace("\\s+".toRegex(), " ")

    /**
     * Valida que no exista duplicidad de DNI, Correo Electrónico o Nombre Completo.
     * @param dniExcluir DNI de la persona que se está editando (en caso de actualización).
     * @return Mensaje de error descriptivo si hay duplicidad, o null si los datos son válidos y únicos.
     */
    fun validarDuplicidad(
        dni: String,
        nombres: String,
        apellidos: String,
        correo: String,
        personasExistentes: List<Persona>,
        dniExcluir: String? = null
    ): String? {
        val cleanDni = dni.trim()
        val cleanNombres = normalizarEspacios(nombres)
        val cleanApellidos = normalizarEspacios(apellidos)
        val cleanCorreo = correo.trim()

        val otras = if (dniExcluir != null) {
            personasExistentes.filter { it.dni != dniExcluir }
        } else {
            personasExistentes
        }

        // 1. Duplicidad de DNI (cuando es nuevo registro)
        if (dniExcluir == null) {
            val duplicadoDni = otras.firstOrNull { it.dni == cleanDni }
            if (duplicadoDni != null) {
                return "Ya existe una persona registrada con el DNI $cleanDni (${duplicadoDni.nombreCompleto})."
            }
        }

        // 2. Duplicidad de Correo Electrónico
        val duplicadoCorreo = otras.firstOrNull {
            it.correoElectronico.trim().equals(cleanCorreo, ignoreCase = true)
        }
        if (duplicadoCorreo != null) {
            return "El correo \"$cleanCorreo\" ya se encuentra registrado para ${duplicadoCorreo.nombreCompleto} (DNI ${duplicadoCorreo.dni})."
        }

        // 3. Duplicidad de Nombre Completo (Nombres y Apellidos)
        val duplicadoNombre = otras.firstOrNull {
            normalizarEspacios(it.nombres).equals(cleanNombres, ignoreCase = true) &&
            normalizarEspacios(it.apellidos).equals(cleanApellidos, ignoreCase = true)
        }
        if (duplicadoNombre != null) {
            return "Ya existe una persona registrada con el nombre \"${duplicadoNombre.nombreCompleto}\" (DNI ${duplicadoNombre.dni})."
        }

        return null
    }

    /** Convierte excepciones tecnicas en mensajes amigables sin exponer detalles internos. */
    fun mapearError(e: Exception, operacion: String, dniActual: String): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("chk_dni_format", ignoreCase = true) ->
                "El DNI debe ser exactamente 8 dígitos numéricos."
            raw.contains("chk_celular_format", ignoreCase = true) ->
                "El celular debe ser exactamente 9 dígitos numéricos."
            raw.contains("fk_persona_profesion", ignoreCase = true) ->
                "La profesión seleccionada no existe en el catálogo."
            raw.contains("correo", ignoreCase = true) && (raw.contains("duplicate", ignoreCase = true) || raw.contains("unique", ignoreCase = true)) ->
                "El correo electrónico ya se encuentra registrado por otra persona."
            raw.contains("duplicate", ignoreCase = true) || raw.contains("unique", ignoreCase = true) || raw.contains("23505") ->
                "Ya existe una persona registrada con el DNI $dniActual."
            raw.contains("network", ignoreCase = true) || raw.contains("timeout", ignoreCase = true) || raw.contains("connection", ignoreCase = true) ->
                "Error de conexión. Revise su acceso a internet e intente nuevamente."
            else ->
                "No se pudo $operacion el registro. Intente nuevamente."
        }
    }
}
