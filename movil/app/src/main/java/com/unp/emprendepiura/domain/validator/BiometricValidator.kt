package com.unp.emprendepiura.domain.validator

/*
 * Archivo: BiometricValidator.kt
 * Proposito: Reglas de negocio y validaciones para registro y login biometrico en EmprendePiura.
 */
object BiometricValidator {

    fun validarDni(dni: String): String? {
        val clean = dni.trim()
        return when {
            clean.isEmpty() -> "Ingresa el DNI del usuario."
            clean.length != 8 || !clean.all { it.isDigit() } ->
                "El DNI debe contener exactamente 8 dígitos numéricos."
            else -> null
        }
    }

    fun validarRegistro(
        dni: String,
        nombre: String,
        telefono: String,
        correo: String,
        contrasena: String,
        huellaToken: String,
        rostroListo: Boolean,
        esModoEdicion: Boolean
    ): String? {
        validarDni(dni)?.let { return it }
        if (nombre.isBlank()) {
            return "El nombre completo es obligatorio."
        }
        val cleanTel = telefono.trim()
        if (cleanTel.isNotEmpty() && (cleanTel.length < 7 || !cleanTel.all { it.isDigit() })) {
            return "Ingresa un número de teléfono válido."
        }
        val cleanMail = correo.trim()
        if (cleanMail.isNotEmpty() && (!cleanMail.contains("@") || !cleanMail.contains("."))) {
            return "Ingresa un correo electrónico válido o déjalo vacío."
        }
        if (!esModoEdicion && contrasena.trim().length < 4) {
            return "La contraseña debe tener al menos 4 caracteres."
        }
        if (esModoEdicion && contrasena.isNotEmpty() && contrasena.trim().length < 4) {
            return "Si deseas cambiar la contraseña, debe tener al menos 4 caracteres."
        }
        if (huellaToken.isBlank()) {
            return "Vincula tu huella dactilar antes de guardar."
        }
        if (!rostroListo) {
            return "Captura y verifica tu rostro antes de guardar."
        }
        return null
    }

    fun mapearError(e: Exception, dni: String): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("DUPLICATE_DNI") || msg.contains("idx_usuarios_dni_unique") ->
                "Ya existe una cuenta registrada con el DNI ${dni.trim()}."
            msg.contains("DUPLICATE_EMAIL") || msg.contains("usuarios_correo_key") ->
                "El correo electrónico ingresado ya está en uso."
            msg.contains("23505") || msg.contains("duplicate", ignoreCase = true) ->
                "El DNI o correo ya se encuentra registrado."
            msg.contains("network", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) ->
                "Sin conexión con el servidor. Verifica tu internet e intenta nuevamente."
            else -> "No se pudo completar la operación en la base de datos."
        }
    }
}
