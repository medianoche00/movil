package com.unp.registroestudiantes.biometria.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Archivo: UsuarioBiometricoDto.kt
 * Proposito: Objetos de transferencia de datos (DTO) para la tabla PostgreSQL usuarios_biometricos.
 */

@Serializable
data class UsuarioBiometricoRow(
    val dni: String,
    val nombre: String,
    val contrasena: String = "",
    val huella: String = "",
    @SerialName("imagen_facial")
    val imagenFacial: String = "",
    @SerialName("vector_facial")
    val vectorFacial: String? = null
)

@Serializable
data class UsuarioBiometricoWrite(
    val dni: String,
    val nombre: String,
    val contrasena: String,
    val huella: String,
    @SerialName("imagen_facial")
    val imagenFacial: String,
    @SerialName("vector_facial")
    val vectorFacial: String? = null
)

/** Transforma el DTO de red en la entidad limpia de dominio. */
fun UsuarioBiometricoRow.toDomain(): UsuarioBiometrico =
    UsuarioBiometrico(
        dni = dni.trim(),
        nombre = nombre,
        contrasena = contrasena,
        huella = huella,
        imagenFacial = imagenFacial,
        vectorFacial = vectorFacial.orEmpty()
    )
