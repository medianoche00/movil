package com.unp.registroestudiantes.persona.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Archivo: Profesion.kt
 * Proposito: Modelo y DTO de la entidad Profesion para el catalogo de seleccion.
 */

/** Modelo de dominio para representar una profesion en la interfaz. */
data class Profesion(
    val codigoProfesion: String,
    val nombre: String
)

/** Fila leida desde la tabla 'profesiones' en Supabase. */
@Serializable
data class ProfesionRow(
    @SerialName("codigo_profesion") val codigoProfesion: String,
    val nombre: String,
    @SerialName("id_facultad") val idFacultad: Int = 0
)

/** Convierte una fila DTO al modelo de dominio Profesion. */
fun ProfesionRow.toProfesion(): Profesion = Profesion(
    codigoProfesion = codigoProfesion,
    nombre = nombre
)
