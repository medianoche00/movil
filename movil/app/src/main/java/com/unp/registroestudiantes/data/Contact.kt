package com.unp.registroestudiantes.data

import java.util.UUID

/**
 * Clase de datos (data class) que modela la entidad Contacto.
 * Contiene: id, nombre, celular y correo.
 */
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val email: String
)
