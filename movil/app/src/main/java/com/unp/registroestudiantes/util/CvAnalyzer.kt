package com.unp.registroestudiantes.util

import java.text.Normalizer

data class Keyword(val name: String, val category: String)

/**
 * Análisis 100% local del CV: busca palabras clave (sin IA externa)
 * y arma la reseña con plantillas de texto.
 */
object CvAnalyzer {
    const val TECH = "tech"
    const val SOFT = "soft"
    const val EXP = "experience"

    private val catalog: List<Keyword> =
        listOf(
            "Kotlin", "Java", "Python", "JavaScript", "TypeScript", "React", "SQL", "Android",
            "Figma", "Git", "C++", "Flutter", "Node.js", "HTML", "CSS", "Power BI", "Excel",
            "Supabase", "Docker"
        ).map { Keyword(it, TECH) } +
        listOf(
            "liderazgo", "trabajo en equipo", "comunicación", "proactividad",
            "responsabilidad", "adaptabilidad", "creatividad", "resolución de problemas"
        ).map { Keyword(it, SOFT) } +
        listOf(
            "practicante", "pasantía", "freelance", "desarrollador", "becario",
            "voluntariado", "prácticas", "analista"
        ).map { Keyword(it, EXP) }

    // Quita tildes y pasa a minúsculas para comparar sin importar acentos/mayúsculas
    private fun norm(s: String) =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    /** Devuelve las palabras clave del catálogo que aparecen como palabra completa en el texto. */
    fun analyze(text: String): List<Keyword> {
        if (text.isBlank()) return emptyList()
        val t = norm(text)
        return catalog.filter { k ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(norm(k.name))}(?![\\p{L}\\p{N}])").containsMatchIn(t)
        }
    }

    private fun List<String>.natural() = when (size) {
        0 -> ""
        1 -> this[0]
        else -> dropLast(1).joinToString(", ") + " y " + last()
    }

    fun buildReview(name: String, age: Int, career: String, found: List<Keyword>, hasCv: Boolean): String {
        val base = "$name es estudiante de $career, $age años."
        if (!hasCv || found.isEmpty())
            return "$base No se adjuntó CV o no se detectaron palabras clave relevantes."

        val tech = found.filter { it.category == TECH }.map { it.name }
        val soft = found.filter { it.category == SOFT }.map { it.name }
        val exp = found.filter { it.category == EXP }.map { it.name }

        val parts = mutableListOf(base)
        if (tech.isNotEmpty())
            parts += "Según su CV, muestra experiencia relacionada con tecnología, con menciones a ${tech.take(5).natural()}."
        if (soft.isNotEmpty())
            parts += "Se identifican indicios de ${soft.take(4).natural()}."
        if (exp.isNotEmpty())
            parts += "Registra antecedentes como ${exp.take(3).natural()}."
        return parts.joinToString(" ")
    }
}
