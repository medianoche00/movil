package com.unp.registroestudiantes.persona.data.model

/*
 * Archivo: Persona.kt
 * Proposito: Modelo de dominio de la entidad Persona (Trabajo N°03).
 */
data class  Persona(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val direccion: String,
    val codigoProfesion: String,
    val celular: String,
    val correoElectronico: String,
    val foto: String?
) {
    /** Nombre completo de la persona */
    val nombreCompleto: String
        get() = "$nombres $apellidos".trim()

    /** Iniciales para el avatar visual */
    val iniciales: String
        get() {
            val n = nombres.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
            val a = apellidos.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
            return if ("$n$a".isNotBlank()) "$n$a" else "?"
        }
}
