package com.unp.registroestudiantes.persona.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Archivo: PersonaDto.kt
 * Proposito: Objetos de transferencia de datos (DTO) para la tabla 'personas' en Supabase.
 */

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
 * Carga util para insertar o actualizar una persona.
 * Omite 'created_at' porque la base de datos lo genera por defecto.
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

/** Convierte una fila DTO de la base de datos al modelo de dominio Persona. */
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
