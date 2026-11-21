package com.unp.emprendepiura.core.network

import com.unp.emprendepiura.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

/*
 * Archivo: SupaClient.kt
 * Proposito: Punto unico de acceso a Supabase (Postgrest, Storage y Realtime WebSockets)
 *            para EmprendePiura.
 */
object SupaClient {

    const val BUCKET_PRODUCTOS = "marketplace-productos"
    const val BUCKET_COMPROBANTES = "marketplace-comprobantes"
    const val BUCKET_ROSTROS = "marketplace-rostros"

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Storage)
        install(Realtime) {
            reconnectDelay = 5.seconds
        }
    }
}
