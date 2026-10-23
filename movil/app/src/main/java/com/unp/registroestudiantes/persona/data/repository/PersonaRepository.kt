package com.unp.registroestudiantes.persona.data.repository

import com.unp.registroestudiantes.data.Supa
import com.unp.registroestudiantes.persona.data.model.Persona
import com.unp.registroestudiantes.persona.data.model.PersonaRow
import com.unp.registroestudiantes.persona.data.model.PersonaWrite
import com.unp.registroestudiantes.persona.data.model.Profesion
import com.unp.registroestudiantes.persona.data.model.ProfesionRow
import com.unp.registroestudiantes.persona.data.model.toPersona
import com.unp.registroestudiantes.persona.data.model.toProfesion
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage

/*
 * Archivo: PersonaRepository.kt
 * Proposito: Repositorio para la gestion de datos de Persona y Profesion con Supabase.
 */
class PersonaRepository {

    /** Obtiene todas las profesiones registradas ordenadas alfabeticamente. */
    suspend fun getProfesiones(): List<Profesion> =
        Supa.client.from("profesiones")
            .select { order("nombre", Order.ASCENDING) }
            .decodeList<ProfesionRow>()
            .map { it.toProfesion() }

    /** Busca una persona por su clave primaria DNI. */
    suspend fun getByDni(dni: String): Persona? =
        Supa.client.from("personas")
            .select { filter { eq("dni", dni) } }
            .decodeList<PersonaRow>()
            .firstOrNull()
            ?.toPersona()

    /** Obtiene el listado completo de personas ordenadas por apellidos. */
    suspend fun getAll(): List<Persona> =
        Supa.client.from("personas")
            .select { order("apellidos", Order.ASCENDING) }
            .decodeList<PersonaRow>()
            .map { it.toPersona() }

    /** Inserta un nuevo registro de persona y retorna la entidad creada. */
    suspend fun insert(data: PersonaWrite): Persona =
        Supa.client.from("personas")
            .insert(data) { select() }
            .decodeSingle<PersonaRow>()
            .toPersona()

    /** Actualiza los datos de la persona identificada por su DNI. */
    suspend fun update(dni: String, data: PersonaWrite): Persona =
        Supa.client.from("personas")
            .update(data) {
                select()
                filter { eq("dni", dni) }
            }
            .decodeSingle<PersonaRow>()
            .toPersona()

    /** Elimina el registro de la persona por DNI. */
    suspend fun delete(dni: String) {
        Supa.client.from("personas")
            .delete { filter { eq("dni", dni) } }
    }

    /**
     * Sube el archivo binario de la fotografia al bucket de Supabase Storage
     * y retorna su URL publica de acceso.
     */
    suspend fun uploadPhoto(dni: String, imageBytes: ByteArray): String {
        val path = "personas/$dni.jpg"
        Supa.client.storage.from(Supa.PERSONAS_PHOTOS_BUCKET)
            .upload(path, imageBytes) { upsert = true }
        return Supa.client.storage.from(Supa.PERSONAS_PHOTOS_BUCKET)
            .publicUrl(path)
    }
}
