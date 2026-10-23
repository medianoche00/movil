package com.unp.registroestudiantes.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Archivo: PersonaRepository.kt
 * Proposito: Repositorio para la gestión de Personas conectado a Supabase (Trabajo N°03).
 *
 * Implementa el patrón Repository encapsulando todas las operaciones CRUD
 * sobre las tablas 'personas' y 'profesiones', y la subida de fotos al bucket.
 */
class PersonaRepository {

    // ------------------------------------------------------------------
    // PROFESIONES - catálogo solo lectura
    // ------------------------------------------------------------------

    /** Obtiene todas las profesiones del catálogo para llenar el DropdownMenu. */
    suspend fun getProfesiones(): List<Profesion> =
        Supa.client.from("profesiones")
            .select { order("nombre", Order.ASCENDING) }
            .decodeList<ProfesionRow>()
            .map { it.toProfesion() }

    // ------------------------------------------------------------------
    // PERSONAS - operaciones CRUD
    // ------------------------------------------------------------------

    /** Busca una persona exactamente por su DNI (llave primaria). */
    suspend fun getByDni(dni: String): Persona? =
        Supa.client.from("personas")
            .select { filter { eq("dni", dni) } }
            .decodeList<PersonaRow>()
            .firstOrNull()
            ?.toPersona()

    /** Obtiene todas las personas ordenadas por apellido ascendente. */
    suspend fun getAll(): List<Persona> =
        Supa.client.from("personas")
            .select { order("apellidos", Order.ASCENDING) }
            .decodeList<PersonaRow>()
            .map { it.toPersona() }

    /**
     * Inserta una nueva persona en la tabla 'personas'.
     * @return La persona creada con todos sus datos confirmados por la BD.
     */
    suspend fun insert(data: PersonaWrite): Persona =
        Supa.client.from("personas")
            .insert(data) { select() }
            .decodeSingle<PersonaRow>()
            .toPersona()

    /**
     * Actualiza los datos de la persona identificada por su DNI.
     * @return La persona actualizada.
     */
    suspend fun update(dni: String, data: PersonaWrite): Persona =
        Supa.client.from("personas")
            .update(data) {
                select()
                filter { eq("dni", dni) }
            }
            .decodeSingle<PersonaRow>()
            .toPersona()

    /**
     * Elimina la persona de la base de datos por su DNI.
     */
    suspend fun delete(dni: String) {
        Supa.client.from("personas")
            .delete { filter { eq("dni", dni) } }
    }

    // ------------------------------------------------------------------
    // STORAGE - subida de foto al bucket 'personas-photos'
    // ------------------------------------------------------------------

    /**
     * Sube los bytes de la foto al bucket 'personas-photos' de Supabase Storage.
     * @param dni DNI de la persona (se usa como nombre de archivo para sobreescribir).
     * @param imageBytes Contenido binario de la imagen JPEG/PNG.
     * @return URL pública de la foto almacenada.
     */
    suspend fun uploadPhoto(dni: String, imageBytes: ByteArray): String {
        val path = "personas/$dni.jpg"
        Supa.client.storage.from(Supa.PERSONAS_PHOTOS_BUCKET)
            .upload(path, imageBytes) { upsert = true }
        return Supa.client.storage.from(Supa.PERSONAS_PHOTOS_BUCKET)
            .publicUrl(path)
    }
}
