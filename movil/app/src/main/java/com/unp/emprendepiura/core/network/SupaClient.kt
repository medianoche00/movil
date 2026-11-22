package com.unp.emprendepiura.core.network

import com.unp.emprendepiura.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/*
 * Archivo: SupaClient.kt
 * Proposito: Punto unico de acceso a Supabase en la nube (Postgrest, Storage y Realtime WSS)
 *            configurado con OkHttp keep-alive para operar entre distintas redes (Wi-Fi / 4G / 5G).
 */
object SupaClient {

    const val BUCKET_PRODUCTOS = "marketplace-productos"
    const val BUCKET_COMPROBANTES = "marketplace-comprobantes"
    const val BUCKET_ROSTROS = "marketplace-rostros"
    const val BUCKET_CHAT = "marketplace-chat"

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        httpEngine = OkHttp.create {
            config {
                pingInterval(15, TimeUnit.SECONDS)
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(0, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
        install(Postgrest)
        install(Storage)
        install(Realtime) {
            reconnectDelay = 3.seconds
            heartbeatInterval = 15.seconds
            connectOnSubscribe = true
            disconnectOnNoSubscriptions = false
            disconnectOnSessionLoss = false
        }
    }
}
