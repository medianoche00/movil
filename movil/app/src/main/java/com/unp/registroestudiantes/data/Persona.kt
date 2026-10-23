package com.unp.registroestudiantes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Archivo: Persona.kt
 * Proposito: Modelado y serializacion de la entidad Persona y Profesion (Trabajo N°03).
 * Conectado con las tablas 'personas' y 'profesiones' en Supabase.
 *
 * Modelo de Datos:
 *   - profesiones (codigo_profesion PK, nombre, id_facultad)
 *   - personas    (dni PK, nombres, apellidos, direccion, codigo_profesion FK,
 *                  celular, correo_electronico, foto)
 */

// ============================================================
// 1. Modelos de Dominio / Interfaz de Usuario (UI)
// ============================================================

/** Representa una profesion del catálogo para mostrarse en el Spinner/DropdownMenu. */
data class Profesion(
    val codigoProfesion: String,
    val nombre: String
)

/** Representa los datos completos de una persona listos para mostrarse en pantalla. */
data class Persona(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val direccion: String,
    val codigoProfesion: String,
    val celular: String,
    val correoElectronico: String,
    val foto: String?           // Path local o URL pública donde está guardada la imagen
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

// ============================================================
// 2. DTOs (Data Transfer Objects) - Mapeo directo con Supabase
// ============================================================

/** Fila leida desde la tabla 'profesiones'. */
@Serializable
data class ProfesionRow(
    @SerialName("codigo_profesion") val codigoProfesion: String,
    val nombre: String,
    @SerialName("id_facultad") val idFacultad: Int = 0
)

/** Fila leida desde la tabla 'personas'. */
@Serializable
data class PersonaRow(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val direccion: String,
    @SerialName("codigo_profesion") val codigoProfesion: String,
    val celular: String,
    @SerialName("correo_electronico") val correoElectronico: String,
    val foto: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Carga útil para insertar o actualizar una persona.
 * No incluye 'created_at' porque la BD lo genera con DEFAULT now().
 */
@Serializable
data class PersonaWrite(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val direccion: String,
    @SerialName("codigo_profesion") val codigoProfesion: String,
    val celular: String,
    @SerialName("correo_electronico") val correoElectronico: String,
    val foto: String? = null
)

// ============================================================
// 3. Mapeadores de conversión (Extension Functions)
// ============================================================

fun ProfesionRow.toProfesion(): Profesion = Profesion(
    codigoProfesion = codigoProfesion,
    nombre = nombre
)

fun PersonaRow.toPersona(): Persona = Persona(
    dni = dni,
    nombres = nombres,
    apellidos = apellidos,
    direccion = direccion,
    codigoProfesion = codigoProfesion,
    celular = celular,
    correoElectronico = correoElectronico,
    foto = foto
)
