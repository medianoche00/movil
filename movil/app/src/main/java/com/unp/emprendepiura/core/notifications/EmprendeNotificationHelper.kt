package com.unp.emprendepiura.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.unp.emprendepiura.MainActivity
import com.unp.emprendepiura.R
import kotlin.math.abs

/*
 * Archivo: EmprendeNotificationHelper.kt
 * Proposito: Gestion modular de notificaciones nativas de Android (canales de Mensajes y Pedidos)
 *            para avisar en vivo nuevos chats, pedidos entrantes y cambios de estado.
 */
object EmprendeNotificationHelper {

    const val CHANNEL_MENSAJES = "emprendepiura_mensajes"
    const val CHANNEL_PEDIDOS = "emprendepiura_pedidos"

    fun crearCanales(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val canalMensajes = NotificationChannel(
            CHANNEL_MENSAJES,
            "Mensajes en tiempo real",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos inmediatos de nuevos mensajes de compradores y emprendedores."
            enableVibration(true)
        }

        val canalPedidos = NotificationChannel(
            CHANNEL_PEDIDOS,
            "Pedidos y entregas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de nuevos pedidos recibidos y cambios de estado de entrega."
            enableVibration(true)
        }

        manager.createNotificationChannel(canalMensajes)
        manager.createNotificationChannel(canalPedidos)
    }

    fun tienePermiso(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun mostrarNotificacionMensaje(
        context: Context,
        conversacionId: String,
        nombreRemitente: String,
        contenido: String
    ) {
        if (!tienePermiso(context)) return
        crearCanales(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            abs(conversacionId.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MENSAJES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(nombreRemitente.ifBlank { "Nuevo mensaje · EmprendePiura" })
            .setContentText(contenido)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = 2000 + (abs(conversacionId.hashCode()) % 10000)
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    fun mostrarNotificacionPedido(
        context: Context,
        ordenId: String,
        titulo: String,
        detalle: String
    ) {
        if (!tienePermiso(context)) return
        crearCanales(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            abs(ordenId.hashCode()) + 5000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PEDIDOS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(detalle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notifId = 15000 + (abs(ordenId.hashCode()) % 10000)
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }
}
