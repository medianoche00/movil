package com.unp.registroestudiantes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Archivo: Models.kt
 * Proposito: Definicion de los modelos de datos de la aplicacion.
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. 'data class' en Kotlin vs POJO / JavaBeans en Java:
 *    En Java, para una clase con 10 atributos se requerian mas de 80 lineas de codigo:
 *    campos privados, constructor con todos los parametros, getters, setters,
 *    equals(), hashCode(), y toString().
 *    En Kotlin, la palabra reservada 'data class' genera automaticamente todo eso
 *    en tiempo de compilacion, incluyendo la funcion .copy() para clonar objetos.
 *
 * 2. Sistema de tipos nulos (Null Safety):
 *    - 'String': Obligatoriamente debe tener texto; el compilador no permite asignar null.
 *    - 'String?': Con signo de interrogacion indica que puede contener null.
 *    En Java no existe distincion en tiempo de compilacion, lo que provoca NullPointerException.
 *
 * 3. Separacion en dos capas: Modelos de UI vs DTOs (Data Transfer Objects):
 *    - DTOs (SkillDto, StudentRow, StudentWrite): Coinciden con el esquema de tablas SQL
 *      y los nombres en snake_case devueltos por la API REST de Supabase.
 *    - Modelos de UI (Student, Skill): Limpios, con tipos ya normalizados y faciles
 *      de consumir por los componentes de pantalla sin acoplarse al motor de base de datos.
 *
 * 4. Anotaciones de serializacion:
 *    - @Serializable: Permite convertir objetos Kotlin a JSON y viceversa.
 *    - @SerialName("columna_bd"): Equivalente a @JsonProperty("columna_bd") en Jackson
 *      o @SerializedName en Gson. Mapea la columna snake_case de SQL al atributo camelCase.
 *
 * 5. Funciones de extension:
 *    'fun StudentRow.toStudent()': Agrega un metodo de transformacion a la clase StudentRow
 *    sin tener que modificar la clase original ni heredar de ella. En Java equivaldria
 *    a un metodo estatico en una clase de utilidades: 'public static Student toStudent(StudentRow row)'.
 */

// ============================================================
// 1. Modelos de Dominio / Interfaz de Usuario (UI)
// ============================================================

/** Representa una aptitud o competencia asociada a un estudiante. */
data class Skill(
    val id: Long,
    val name: String,
    val category: String
)

/** Representa los datos completos de un estudiante listos para mostrarse en pantalla. */
data class Student(
    val id: String,
    val name: String,
    val age: Int,
    val career: String,
    val email: String?,          // Opcional: puede ser null si no se registro
    val photoUrl: String?,       // URL publica de la foto en Supabase Storage
    val cvUrl: String?,          // URL publica del CV en Supabase Storage
    val backgroundText: String?, // Texto extraido del CV mediante OCR o PDFBox
    val review: String,          // Resena automatica generada
    val skills: List<Skill>      // Lista de aptitudes asociadas
)

// ============================================================
// 2. DTOs (Data Transfer Objects) - Mapeo directo con Supabase
// ============================================================

/** Representa una fila leida de la tabla 'skills'. */
@Serializable
data class SkillDto(
    val id: Long,
    val name: String,
    val category: String
)

/** Representa los datos para insertar una nueva aptitud en 'skills'. */
@Serializable
data class SkillInsert(
    val name: String,
    val category: String
)

/**
 * Representa la union relacional de la tabla intermedia 'student_skills'
 * con la tabla 'skills' anidada. Supabase devuelve la estructura:
 * { "skills": { "id": 1, "name": "Kotlin", "category": "tech" } }
 */
@Serializable
data class StudentSkillJoin(
    val skills: SkillDto? = null
)

/**
 * Representa la lectura de una fila de la tabla 'students' junto a
 * su relacion de aptitudes anidadas.
 */
@Serializable
data class StudentRow(
    val id: String,
    val name: String,
    val age: Int,
    val career: String,
    val email: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("cv_url") val cvUrl: String? = null,
    @SerialName("background_text") val backgroundText: String? = null,
    val review: String? = null,
    @SerialName("student_skills") val studentSkills: List<StudentSkillJoin> = emptyList()
)

/**
 * Representa la carga util para insertar o actualizar en 'students'.
 * No incluye 'id' ni 'created_at' porque la base de datos los genera automaticamente.
 */
@Serializable
data class StudentWrite(
    val name: String,
    val age: Int,
    val career: String,
    val email: String?,
    @SerialName("photo_url") val photoUrl: String?,
    @SerialName("cv_url") val cvUrl: String?,
    @SerialName("background_text") val backgroundText: String?,
    val review: String
)

/** Representa un registro en la tabla intermedia 'student_skills'. */
@Serializable
data class StudentSkillLink(
    @SerialName("student_id") val studentId: String,
    @SerialName("skill_id") val skillId: Long
)

// ============================================================
// 3. Mapeador de conversion (Extension Function)
// ============================================================

/**
 * Convierte el objeto de transporte (StudentRow) en el modelo de dominio (Student).
 * En Java equivaldria a:
 *   public static Student toStudent(StudentRow row) { ... }
 */
fun StudentRow.toStudent(): Student = Student(
    id = id,
    name = name,
    age = age,
    career = career,
    email = email,
    photoUrl = photoUrl,
    cvUrl = cvUrl,
    backgroundText = backgroundText,
    // .orEmpty(): Si review es null devuelve "", equivalente a: review != null ? review : ""
    review = review.orEmpty(),
    // mapNotNull: Filtra los nulos y transforma los elementos no nulos en una sola pasada
    skills = studentSkills.mapNotNull { it.skills }.map { Skill(it.id, it.name, it.category) }
)
