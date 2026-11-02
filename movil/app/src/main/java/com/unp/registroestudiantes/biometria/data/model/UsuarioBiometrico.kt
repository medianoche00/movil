package com.unp.registroestudiantes.biometria.data.model

/*
 * Archivo: UsuarioBiometrico.kt
 * Proposito: Entidad de dominio que representa un usuario con credenciales biometricas registradas.
 */
data class UsuarioBiometrico(
    val dni: String,
    val nombre: String,
    val contrasena: String,
    val huella: String,
    val imagenFacial: String,
    val vectorFacial: String = ""
) {
    /** Obtiene hasta dos iniciales en mayuscula a partir del nombre completo. */
    val iniciales: String
        get() {
            val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val primera = partes.getOrNull(0)?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            val segunda = partes.getOrNull(1)?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            return (primera + segunda).ifEmpty { "US" }
        }
}
