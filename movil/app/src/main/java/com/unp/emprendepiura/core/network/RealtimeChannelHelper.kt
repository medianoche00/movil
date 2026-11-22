package com.unp.emprendepiura.core.network

import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

/*
 * Archivo: RealtimeChannelHelper.kt
 * Proposito: Ciclo de vida resiliente para canales Supabase Realtime WSS.
 *            Garantiza reconexion automatica entre redes distintas (Wi-Fi / 4G / 5G),
 *            re-suscripcion limpia y eliminacion segura sin colisiones de instancias.
 */
object RealtimeChannelHelper {

    private val channelCounter = AtomicLong(0L)

    fun uniqueChannelId(prefix: String): String = "${prefix}_${channelCounter.incrementAndGet()}"

    fun estaConectado(): Boolean =
        SupaClient.client.realtime.status.value == Realtime.Status.CONNECTED

    suspend fun limpiarCanalResidualPorTopic(channelId: String) {
        SupaClient.client.realtime.subscriptions.values
            .filter { it.topic == channelId || it.topic.endsWith(":$channelId") }
            .forEach { residual ->
                runCatching { SupaClient.client.realtime.removeChannel(residual) }
            }
    }

    suspend fun removerCanalSiCoincide(channel: RealtimeChannel) {
        val registrado = SupaClient.client.realtime.subscriptions[channel.topic]
        if (registrado === channel) {
            runCatching { SupaClient.client.realtime.removeChannel(channel) }
        } else {
            runCatching { channel.unsubscribe() }
        }
    }

    /**
     * Mantiene el canal suscrito de forma persistente mientras la corrutina siga activa.
     * Si el telefono cambia de red o pierde conectividad momentaneamente, reintenta
     * automaticamente y ejecuta [onSubscribed] cada vez que la suscripcion queda activa.
     */
    suspend fun suscribirConReintentos(
        channel: RealtimeChannel,
        onSubscribed: suspend (esReconexion: Boolean) -> Unit = {}
    ) {
        var intento = 0
        var yaSuscritoAntes = false

        while (currentCoroutineContext().isActive) {
            try {
                when (channel.status.value) {
                    RealtimeChannel.Status.UNSUBSCRIBED -> {
                        if (SupaClient.client.realtime.status.value == Realtime.Status.DISCONNECTED) {
                            runCatching { SupaClient.client.realtime.connect() }
                        }
                        channel.subscribe(blockUntilSubscribed = true)
                    }
                    RealtimeChannel.Status.SUBSCRIBING -> {
                        withTimeoutOrNull(8_000L) {
                            channel.status.first { it != RealtimeChannel.Status.SUBSCRIBING }
                        }
                    }
                    else -> Unit
                }

                if (channel.status.value == RealtimeChannel.Status.SUBSCRIBED) {
                    intento = 0
                    val esReconexion = yaSuscritoAntes
                    yaSuscritoAntes = true
                    runCatching { onSubscribed(esReconexion) }

                    // Suspender sin consumir CPU hasta detectar caida o reconexion
                    channel.status.first { it != RealtimeChannel.Status.SUBSCRIBED }

                    // Si el SDK esta reconectando automaticamente, esperar brevemente
                    if (channel.status.value == RealtimeChannel.Status.SUBSCRIBING) {
                        withTimeoutOrNull(6_000L) {
                            channel.status.first { it != RealtimeChannel.Status.SUBSCRIBING }
                        }
                    }
                } else {
                    intento++
                    delay((intento * 2_000L).coerceAtMost(6_000L))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                intento++
                delay((intento * 2_500L).coerceAtMost(8_000L))
            }
        }
    }
}
