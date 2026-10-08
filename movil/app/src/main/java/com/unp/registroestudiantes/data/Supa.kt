package com.unp.registroestudiantes.data

import com.unp.registroestudiantes.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/*
 * Archivo: Supa.kt
 * Proposito: Configuracion centralizada y punto de acceso unico al cliente de Supabase.
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. Declaracion 'object' (Patron Singleton en una palabra):
 *    En Java tradicional, para crear una instancia unica (Singleton) necesitabas:
 *      public class Supa {
 *          private static Supa instance;
 *          private Supa() {}
 *          public static synchronized Supa getInstance() { ... }
 *      }
 *    En Kotlin, la palabra reservada 'object' define una clase y crea su unica
 *    instancia de forma perezosa (lazy) y segura para hilos en una sola linea.
 *
 * 2. 'const val' vs 'val':
 *    - const val: Constante en tiempo de compilacion. En Java equivale exactamente
 *      a 'public static final String PHOTOS_BUCKET = "..."'.
 *    - val: Variable inmutable (de solo lectura) asignada en tiempo de ejecucion.
 *      En Java equivale a 'final Tipo variable = ...'.
 *
 * 3. Acceso a variables de entorno con BuildConfig:
 *    BuildConfig.SUPABASE_URL y BuildConfig.SUPABASE_KEY se inyectan en tiempo
 *    de compilacion desde local.properties mediante build.gradle.kts.
 *    Esto evita quemar claves privadas directamente en el codigo fuente.
 *
 * 4. Modulos instalados en el cliente Supabase:
 *    - Postgrest: Modulo ORM/cliente REST para consultas SQL (SELECT, INSERT, UPDATE, DELETE).
 *    - Storage: Modulo para subir y descargar archivos binarios (fotos JPEG y documentos PDF).
 */
object Supa {

    // Nombres exactos de los buckets de almacenamiento creados en el dashboard de Supabase
    const val PHOTOS_BUCKET = "student-photos"
    const val CVS_BUCKET = "student-cvs"

    // Cliente global configurado con las credenciales del proyecto
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        // En Java esto seria registrar interceptores o plugins en una factoria de clientes HTTP
        install(Postgrest)
        install(Storage)
    }
}
