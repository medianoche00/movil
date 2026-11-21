package com.unp.emprendepiura.data.dto

import com.unp.emprendepiura.domain.model.RolUsuario
import com.unp.emprendepiura.domain.model.Usuario
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioRowDto(
    val id: String = "",
    val dni: String,
    val nombre: String,
    val correo: String = "",
    val telefono: String = "",
    @SerialName("password_hash")
    val passwordHash: String = "",
    val rol: String = "COMPRADOR",
    @SerialName("zona_piura")
    val zonaPiura: String = "Campus UNP",
    val huella: String = "",
    @SerialName("imagen_facial")
    val imagenFacial: String = "",
    @SerialName("vector_facial")
    val vectorFacial: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val activo: Boolean = true
)

@Serializable
data class UsuarioUpsertDto(
    val dni: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    @SerialName("password_hash")
    val passwordHash: String,
    @SerialName("zona_piura")
    val zonaPiura: String,
    val huella: String,
    @SerialName("imagen_facial")
    val imagenFacial: String,
    @SerialName("vector_facial")
    val vectorFacial: String,
    @SerialName("avatar_url")
    val avatarUrl: String
)

fun UsuarioRowDto.toDomain(): Usuario = Usuario(
    id = id,
    dni = dni.trim(),
    nombre = nombre,
    correo = correo,
    telefono = telefono,
    passwordHash = passwordHash,
    rol = RolUsuario.fromRaw(rol),
    zonaPiura = zonaPiura,
    huella = huella,
    imagenFacial = imagenFacial,
    vectorFacial = vectorFacial.orEmpty(),
    avatarUrl = avatarUrl.orEmpty(),
    activo = activo
)
