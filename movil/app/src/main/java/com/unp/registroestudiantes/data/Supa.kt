package com.unp.registroestudiantes.data

import com.unp.registroestudiantes.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/** Cliente único de Supabase (base de datos + almacenamiento de archivos). */
object Supa {
    const val PHOTOS_BUCKET = "photos"
    const val CVS_BUCKET = "cvs"

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }
}
