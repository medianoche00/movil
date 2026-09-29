package com.unp.registroestudiantes.data

import com.unp.registroestudiantes.util.Keyword
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage

/**
 * Único punto que habla con Supabase.
 * El ViewModel llama a estas funciones; las pantallas nunca tocan la red directamente.
 */
class StudentRepository {

    // Trae el estudiante y, por la tabla puente, sus skills en una sola consulta.
    private val select = Columns.raw("*, student_skills(skills(id,name,category))")

    suspend fun getAll(): List<Student> =
        Supa.client.from("students")
            .select(select) { order("created_at", Order.DESCENDING) }
            .decodeList<StudentRow>()
            .map { it.toStudent() }

    suspend fun getById(id: String): Student =
        Supa.client.from("students")
            .select(select) { filter { eq("id", id) } }
            .decodeSingle<StudentRow>()
            .toStudent()

    suspend fun create(data: StudentWrite, skillIds: List<Long>): Student {
        val row = Supa.client.from("students")
            .insert(data) { select() }
            .decodeSingle<StudentRow>()
        linkSkills(row.id, skillIds)
        return getById(row.id)
    }

    suspend fun update(id: String, data: StudentWrite, skillIds: List<Long>): Student {
        Supa.client.from("students").update(data) { filter { eq("id", id) } }
        // Reemplazamos las aptitudes: borrar las anteriores e insertar las nuevas
        Supa.client.from("student_skills").delete { filter { eq("student_id", id) } }
        linkSkills(id, skillIds)
        return getById(id)
    }

    suspend fun delete(id: String) {
        Supa.client.from("student_skills").delete { filter { eq("student_id", id) } }
        Supa.client.from("students").delete { filter { eq("id", id) } }
    }

    private suspend fun linkSkills(studentId: String, skillIds: List<Long>) {
        if (skillIds.isEmpty()) return
        Supa.client.from("student_skills")
            .insert(skillIds.distinct().map { StudentSkillLink(studentId, it) })
    }

    /** Se asegura de que cada palabra clave detectada exista en `skills` y devuelve sus ids. */
    suspend fun ensureSkills(found: List<Keyword>): List<Long> {
        if (found.isEmpty()) return emptyList()
        val table = Supa.client.from("skills")
        var all = table.select().decodeList<SkillDto>()
        val known = all.map { it.name.lowercase() }.toSet()
        val missing = found.filter { it.name.lowercase() !in known }
        if (missing.isNotEmpty()) {
            table.insert(missing.map { SkillInsert(it.name, it.category) })
            all = table.select().decodeList<SkillDto>()
        }
        return found.mapNotNull { k -> all.firstOrNull { it.name.equals(k.name, ignoreCase = true) }?.id }
    }

    /** Sube un archivo a un bucket público y devuelve su URL. */
    suspend fun uploadFile(bucket: String, path: String, bytes: ByteArray): String {
        val api = Supa.client.storage.from(bucket)
        api.upload(path, bytes) { upsert = true }
        return api.publicUrl(path)
    }
}
