package com.unp.registroestudiantes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/*
 * Archivo: Contact.kt
 * Proposito: Modelado y serializacion de la entidad Contacto para la agenda estilo WhatsApp.
 * Conectado con la tabla 'contacts' en Supabase.
 */

// ============================================================
// 1. Modelo de Dominio / UI
// ============================================================

/**
 * Representa los datos de un contacto en la aplicacion listos para la interfaz grafica.
 */
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String,
    val username: String? = null,
    val countryCode: String = "+51",
    val phone: String,
    val email: String? = null,
    val syncToPhone: Boolean = false,
    val createdAt: String? = null
) {
    /** Nombre completo del contacto */
    val fullName: String
        get() = "$firstName $lastName".trim()

    /** Telefono con formato internacional */
    val fullPhone: String
        get() = "$countryCode $phone"

    /** Nombre de usuario con prefijo @ si existe */
    val displayUsername: String?
        get() = username?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("@")) it else "@$it" }

    /** Iniciales para el avatar visual (ej. 'JP' para Juan Pérez) */
    val initials: String
        get() {
            val f = firstName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
            val l = lastName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
            val res = "$f$l"
            return if (res.isNotBlank()) res else "?"
        }
}

// ============================================================
// 2. DTOs (Data Transfer Objects) - Mapeo directo con Supabase
// ============================================================

/**
 * Representa una fila leida desde la tabla 'contacts' de Supabase.
 */
@Serializable
data class ContactRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val username: String? = null,
    @SerialName("country_code") val countryCode: String = "+51",
    val phone: String,
    val email: String? = null,
    @SerialName("sync_to_phone") val syncToPhone: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Representa la carga util para insertar un nuevo contacto en Supabase.
 * No incluye 'id' ni 'created_at' porque los genera la base de datos con DEFAULT.
 */
@Serializable
data class ContactInsert(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val username: String? = null,
    @SerialName("country_code") val countryCode: String = "+51",
    val phone: String,
    val email: String? = null,
    @SerialName("sync_to_phone") val syncToPhone: Boolean = false
)

// ============================================================
// 3. Mapeador de conversion
// ============================================================

/**
 * Convierte el DTO leido de Supabase (ContactRow) al modelo de dominio (Contact).
 */
fun ContactRow.toContact(): Contact = Contact(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    countryCode = countryCode,
    phone = phone,
    email = email,
    syncToPhone = syncToPhone,
    createdAt = createdAt
)
