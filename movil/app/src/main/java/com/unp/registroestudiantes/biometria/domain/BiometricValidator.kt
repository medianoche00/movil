package com.unp.registroestudiantes.biometria.domain

/*
 * Archivo: BiometricValidator.kt
 * Proposito: Reglas de negocio y validaciones de entrada para autenticacion y registro biometrico.
 */
object BiometricValidator {

    fun validarDni(dni: String): String? {
        val clean = dni.trim()
        return when {
            clean.isEmpty() -> "Ingrese el DNI del usuario."
            clean.length != 8 || !clean.all { it.isDigit() } ->
                "El DNI debe contener exactamente 8 dígitos numéricos."
            else -> null
        }
    }

    fun validarLoginContrasena(dni: String, contrasena: String): String? {
        validarDni(dni)?.let { return it }
        if (contrasena.isBlank()) {
            return "Ingrese su contraseña o utilice los controles de Huella o Reconocimiento Facial."
        }
        return null
    }

    fun validarRegistro(
        dni: String,
        nombre: String,
        contrasena: String,
        huellaToken: String,
        rostroListo: Boolean
    ): String? {
        validarDni(dni)?.let { return it }
        if (nombre.isBlank()) {
            return "El nombre completo del usuario es obligatorio."
        }
        if (contrasena.trim().length < 4) {
            return "La contraseña debe tener al menos 4 caracteres."
        }
        if (huellaToken.isBlank()) {
            return "Debe vincular la huella dactilar antes de guardar el usuario."
        }
        if (!rostroListo) {
            return "Debe capturar y verificar la imagen para reconocimiento facial."
        }
        return null
    }

    fun mapearError(e: Exception, dni: String): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("DUPLICATE_DNI") || msg.contains("23505") || msg.contains("duplicate", ignoreCase = true) ->
                "Ya existe un usuario registrado con el DNI ${dni.trim()}."
            msg.contains("network", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) ->
                "Sin conexión con el servidor. Verifique su red e intente nuevamente."
            else -> "No se pudo completar la operación en la base de datos. Verifique los datos e intente nuevamente."
        }
    }
}
