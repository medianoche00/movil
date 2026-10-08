package com.unp.registroestudiantes.data

import com.unp.registroestudiantes.util.Keyword
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage

/*
 * Archivo: StudentRepository.kt
 * Proposito: Implementacion del patron Repositorio (Repository Pattern).
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. Patron Repositorio:
 *    Es la capa intermedia entre la fuente de datos externa (Supabase) y la logica
 *    de la aplicacion (ViewModel). Las pantallas de interfaz nunca deben hacer
 *    consultas de red directamente; siempre delegan esta responsabilidad al repositorio.
 *
 * 2. Funciones suspendibles ('suspend fun'):
 *    En Java clasico, una peticion de red asincrona requeria Threads, Callbacks,
 *    AsyncTask (obsoleto) o CompletableFuture.
 *    En Kotlin, la palabra reservada 'suspend' indica que la funcion puede pausarse
 *    mientras espera la respuesta del servidor sin congelar el hilo principal de la UI,
 *    y reanudarse cuando los datos esten listos. El codigo se escribe de forma
 *    secuencial y facil de leer, como si fuera sincrono.
 *
 * 3. Inferencia de tipos y lambdas con receptor:
 *    La expresion 'Supa.client.from("students").select(select) { ... }' pasa un bloque
 *    de configuracion. Es similar a usar el patron Builder en Java, pero con una
 *    sintaxis mucho mas concisa.
 *
 * 4. Deserializacion con decodeList<T>() y decodeSingle<T>():
 *    Convierte la respuesta JSON de Supabase en objetos de datos fuertemente tipados
 *    en una sola instruccion, equivalente a ObjectMapper.readValue() de Jackson en Java.
 *
 * 5. Operaciones de colecciones (map, filter, distinct):
 *    Equivalen a la API Stream de Java 8:
 *    - .map { it.toStudent() } equivale a .stream().map(row -> toStudent(row)).collect(...)
 *    - .distinct() equivale a .stream().distinct().collect(...)
 */
class StudentRepository {

    // Consulta SQL relacional: selecciona todos los campos de students (*)
    // y anida las skills a traves de la tabla intermedia student_skills
    private val select = Columns.raw("*, student_skills(skills(id,name,category))")

    /**
     * Obtiene la lista completa de estudiantes ordenados por fecha de creacion descendente.
     * En Java equivaldria a: public List<Student> getAll() throws Exception
     */
    suspend fun getAll(): List<Student> =
        Supa.client.from("students")
            .select(select) { order("created_at", Order.DESCENDING) }
            .decodeList<StudentRow>()
            .map { it.toStudent() }

    /**
     * Obtiene un unico estudiante por su identificador UUID.
     */
    suspend fun getById(id: String): Student =
        Supa.client.from("students")
            .select(select) { filter { eq("id", id) } }
            .decodeSingle<StudentRow>()
            .toStudent()

    /**
     * Crea un estudiante en la tabla 'students', asocia sus aptitudes en 'student_skills'
     * y retorna el estudiante completo con su nuevo ID generado.
     */
    suspend fun create(data: StudentWrite, skillIds: List<Long>): Student {
        val row = Supa.client.from("students")
            .insert(data) { select() }
            .decodeSingle<StudentRow>()
        linkSkills(row.id, skillIds)
        return getById(row.id)
    }

    /**
     * Actualiza los datos de un estudiante existente y sincroniza sus aptitudes.
     */
    suspend fun update(id: String, data: StudentWrite, skillIds: List<Long>): Student {
        Supa.client.from("students").update(data) { filter { eq("id", id) } }
        // Reemplazo de aptitudes: elimina las relaciones anteriores y registra las nuevas
        Supa.client.from("student_skills").delete { filter { eq("student_id", id) } }
        linkSkills(id, skillIds)
        return getById(id)
    }

    /**
     * Elimina un estudiante y sus relaciones asociadas en la base de datos.
     */
    suspend fun delete(id: String) {
        Supa.client.from("student_skills").delete { filter { eq("student_id", id) } }
        Supa.client.from("students").delete { filter { eq("id", id) } }
    }

    /**
     * Vincula una lista de IDs de aptitudes a un estudiante en la tabla intermedia 'student_skills'.
     */
    private suspend fun linkSkills(studentId: String, skillIds: List<Long>) {
        if (skillIds.isEmpty()) return
        Supa.client.from("student_skills")
            .insert(skillIds.distinct().map { StudentSkillLink(studentId, it) })
    }

    /**
     * Verifica que cada palabra clave detectada exista en la tabla 'skills'.
     * Si alguna no existe, la inserta automaticamente y retorna los IDs de todas.
     */
    suspend fun ensureSkills(found: List<Keyword>): List<Long> {
        if (found.isEmpty()) return emptyList()

        val table = Supa.client.from("skills")
        var all = table.select().decodeList<SkillDto>()
        val known = all.map { it.name.lowercase() }.toSet()

        // Filtra palabras clave que no esten aun en la base de datos
        val missing = found.filter { it.name.lowercase() !in known }
        if (missing.isNotEmpty()) {
            table.insert(missing.map { SkillInsert(it.name, it.category) })
            // Recarga el catalogo actualizado
            all = table.select().decodeList<SkillDto>()
        }

        // Retorna los IDs correspondientes a las palabras clave encontradas
        return found.mapNotNull { k ->
            all.firstOrNull { it.name.equals(k.name, ignoreCase = true) }?.id
        }
    }

    /**
     * Sube un archivo binario (ByteArray) a un bucket publico de Supabase Storage
     * y retorna la URL publica para visualizarlo o descargarlo.
     */
    suspend fun uploadFile(bucket: String, path: String, bytes: ByteArray): String {
        val api = Supa.client.storage.from(bucket)
        // upsert = true: si el archivo con ese nombre ya existia, lo sobrescribe
        api.upload(path, bytes) { upsert = true }
        return api.publicUrl(path)
    }
}
