package com.unp.registroestudiantes.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/*
 * Archivo: ContactRepository.kt
 * Proposito: Repositorio para la gestion de contactos conectado a Supabase.
 *
 * Implementa el patron Repository encapsulando las operaciones CRUD sobre la tabla 'contacts'.
 */
class ContactRepository {

    /**
     * Obtiene la lista completa de contactos desde Supabase ordenados por fecha de creacion descendente.
     */
    suspend fun getAll(): List<Contact> =
        Supa.client.from("contacts")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<ContactRow>()
            .map { it.toContact() }

    /**
     * Inserta un nuevo contacto en la tabla 'contacts' y retorna el contacto creado con su UUID generado.
     */
    suspend fun create(data: ContactInsert): Contact =
        Supa.client.from("contacts")
            .insert(data) { select() }
            .decodeSingle<ContactRow>()
            .toContact()

    /**
     * Actualiza los datos de un contacto existente en la tabla 'contacts' de Supabase.
     */
    suspend fun update(id: String, data: ContactInsert): Contact =
        Supa.client.from("contacts")
            .update(data) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<ContactRow>()
            .toContact()

    /**
     * Elimina un contacto de la base de datos por su identificador UUID.
     */
    suspend fun delete(id: String) {
        Supa.client.from("contacts")
            .delete { filter { eq("id", id) } }
    }
}
