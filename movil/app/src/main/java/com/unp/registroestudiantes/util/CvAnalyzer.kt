package com.unp.registroestudiantes.util

import java.text.Normalizer

/*
 * Archivo: CvAnalyzer.kt
 * Proposito: Analisis local de texto de CVs para extraccion de palabras clave
 *            y generacion algoritmica de resenas estudiantiles sin costo ni APIs externas.
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. Declaracion 'object':
 *    Crea una clase singleton accesible directamente como 'CvAnalyzer.analyze(...)',
 *    sin necesidad de instanciar ni usar un constructor privado como en Java.
 *
 * 2. Colecciones inmutables y operador de concatenacion '+':
 *    En Kotlin, 'listOf(...) + listOf(...)' une dos listas creando una nueva lista inmutable.
 *    En Java tradicional habria que crear una nueva ArrayList y usar list1.addAll(list2).
 *
 * 3. Normalizacion de texto:
 *    Normalizer.normalize descompone caracteres acentuados (NFD), permitiendo
 *    remover tildes con una expresion regular para comparar palabras como 'practicas'
 *    y 'prácticas' sin distincion.
 *
 * 4. Expresiones Regulares con Delimitadores de Palabra:
 *    '(?<![\p{L}\p{N}])' y '(?![\p{L}\p{N}])' verifican que la palabra coincida completa,
 *    evitando falsos positivos (por ejemplo, que la palabra 'C' coincida dentro de 'CSS').
 *
 * 5. Expresion 'when':
 *    Reemplazo de la sentencia 'switch' de Java, pero capaz de evaluar expresiones,
 *    rangos y devolver un valor directamente como expresion.
 *
 * 6. Funciones de extension privadas:
 *    'fun List<String>.natural()' agrega el metodo de formato natural a cualquier lista
 *    de cadenas dentro del archivo, formateando [A, B, C] como "A, B y C".
 */

/** Representa una palabra clave reconocida junto a su categoria funcional. */
data class Keyword(
    val name: String,
    val category: String
)

object CvAnalyzer {

    // Constantes de categorias (coinciden con la restriccion CHECK en la tabla 'skills' de PostgreSQL)
    const val TECH = "tech"
    const val SOFT = "soft"
    const val EXP = "experience"

    // Catalogo predefinido de competencias tecnicas, habilidades blandas y roles academicos
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

    /**
     * Remueve acentos ortograficos y convierte el texto a minusculas
     * para permitir comparaciones insensibles a mayusculas y tildes.
     */
    private fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()

    /**
     * Busca en el texto todas las palabras clave del catalogo que aparezcan
     * como palabras completas y retorna la lista de coincidencias.
     */
    fun analyze(text: String): List<Keyword> {
        if (text.isBlank()) return emptyList()

        val normalizedText = norm(text)

        // Filtra del catalogo aquellas palabras presentes en el texto
        return catalog.filter { k ->
            val regex = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(norm(k.name))}(?![\\p{L}\\p{N}])")
            regex.containsMatchIn(normalizedText)
        }
    }

    /**
     * Da formato gramatical natural a una lista de elementos:
     * Ejemplo: ["Java", "Kotlin", "SQL"] se convierte en "Java, Kotlin y SQL".
     */
    private fun List<String>.natural(): String = when (size) {
        0 -> ""
        1 -> this[0]
        else -> dropLast(1).joinToString(", ") + " y " + last()
    }

    /**
     * Construye una resena textual descriptiva para el estudiante
     * basada en los datos demograficos y las palabras clave extraidas de su CV.
     */
    fun buildReview(
        name: String,
        age: Int,
        career: String,
        found: List<Keyword>,
        hasCv: Boolean
    ): String {
        val base = "$name es estudiante de $career, $age años."

        if (!hasCv || found.isEmpty()) {
            return "$base No se adjuntó CV o no se detectaron palabras clave relevantes."
        }

        // Clasifica las competencias encontradas por tipo
        val tech = found.filter { it.category == TECH }.map { it.name }
        val soft = found.filter { it.category == SOFT }.map { it.name }
        val exp = found.filter { it.category == EXP }.map { it.name }

        val parts = mutableListOf(base)

        if (tech.isNotEmpty()) {
            parts += "Según su CV, muestra experiencia relacionada con tecnología, con menciones a ${tech.take(5).natural()}."
        }
        if (soft.isNotEmpty()) {
            parts += "Se identifican indicios de ${soft.take(4).natural()}."
        }
        if (exp.isNotEmpty()) {
            parts += "Registra antecedentes como ${exp.take(3).natural()}."
        }

        // Une las oraciones separadas por espacios
        return parts.joinToString(" ")
    }
}
