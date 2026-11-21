package com.unp.emprendepiura.domain.model

/*
 * Archivo: Usuario.kt
 * Proposito: Entidad unificada de usuario en EmprendePiura (roles + credenciales biometricas 1:1).
 */
enum class RolUsuario(val etiqueta: String) {
    COMPRADOR("Comprador"),
    EMPRENDEDOR("Emprendedor"),
    ADMIN("Administrador");

    companion object {
        fun fromRaw(raw: String): RolUsuario =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: COMPRADOR
    }
}

data class Usuario(
    val id: String = "",
    val dni: String,
    val nombre: String,
    val correo: String,
    val telefono: String = "",
    val passwordHash: String = "",
    val rol: RolUsuario = RolUsuario.COMPRADOR,
    val zonaPiura: String = "Campus UNP",
    val huella: String = "",
    val imagenFacial: String = "",
    val vectorFacial: String = "",
    val avatarUrl: String = "",
    val activo: Boolean = true
) {
    val iniciales: String
        get() {
            val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val primera = partes.getOrNull(0)?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            val segunda = partes.getOrNull(1)?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            return (primera + segunda).ifEmpty { "EP" }
        }

    val fotoPerfil: String
        get() = imagenFacial.ifBlank { avatarUrl }
}
