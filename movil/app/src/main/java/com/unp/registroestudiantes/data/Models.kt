package com.unp.registroestudiantes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Modelos de UI (lo que usan las pantallas) ----------
data class Skill(val id: Long, val name: String, val category: String)

data class Student(
    val id: String,
    val name: String,
    val age: Int,
    val career: String,
    val email: String?,
    val photoUrl: String?,
    val cvUrl: String?,
    val backgroundText: String?,
    val review: String,
    val skills: List<Skill>
)

// ---------- DTOs (espejo de las tablas de Supabase) ----------
@Serializable
data class SkillDto(val id: Long, val name: String, val category: String)

@Serializable
data class SkillInsert(val name: String, val category: String)

/** Fila de la tabla puente con la skill anidada: student_skills(skills(...)) */
@Serializable
data class StudentSkillJoin(val skills: SkillDto? = null)

/** Lectura de students (+ skills anidadas). */
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

/** Escritura (insert/update) en students. Sin id ni created_at: los pone la BD. */
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

@Serializable
data class StudentSkillLink(
    @SerialName("student_id") val studentId: String,
    @SerialName("skill_id") val skillId: Long
)

fun StudentRow.toStudent() = Student(
    id = id, name = name, age = age, career = career, email = email,
    photoUrl = photoUrl, cvUrl = cvUrl, backgroundText = backgroundText,
    review = review.orEmpty(),
    skills = studentSkills.mapNotNull { it.skills }.map { Skill(it.id, it.name, it.category) }
)
