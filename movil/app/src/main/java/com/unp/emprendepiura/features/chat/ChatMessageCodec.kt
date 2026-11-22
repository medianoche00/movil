package com.unp.emprendepiura.features.chat

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

/*
 * Archivo: ChatMessageCodec.kt
 * Proposito: Codificador retrocompatible para mensajes enriquecidos (foto, nota de voz,
 *            documento, ubicacion, contacto, respuestas citadas y eliminacion individual
 *            "para mi" o "para todos") dentro de public.mensajes.
 */
enum class ChatMediaTipo {
    TEXT,
    IMAGE,
    AUDIO,
    DOCUMENT,
    LOCATION,
    CONTACT,
    DELETED;

    companion object {
        fun fromRaw(raw: String): ChatMediaTipo =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: TEXT
    }
}

@Serializable
data class ChatReplyQuote(
    val messageId: String = "",
    val senderName: String = "",
    val preview: String = ""
)

@Serializable
data class ChatRichPayload(
    val type: String = ChatMediaTipo.TEXT.name,
    val text: String = "",
    val url: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L,
    val durationSec: Int = 0,
    val lat: Double? = null,
    val lng: Double? = null,
    val addressLabel: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val reply: ChatReplyQuote? = null,
    val hiddenFor: List<String> = emptyList()
) {
    val tipo: ChatMediaTipo
        get() = ChatMediaTipo.fromRaw(type)
}

object ChatMessageCodec {

    private const val PREFIX = "EP_CHAT_V1|"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(payload: ChatRichPayload): String {
        if (payload.tipo == ChatMediaTipo.TEXT && payload.reply == null && payload.hiddenFor.isEmpty()) {
            return payload.text.trim()
        }
        return PREFIX + json.encodeToString(payload)
    }

    fun decode(raw: String): ChatRichPayload {
        if (!raw.startsWith(PREFIX)) {
            return ChatRichPayload(type = ChatMediaTipo.TEXT.name, text = raw)
        }
        val body = raw.removePrefix(PREFIX)
        return runCatching {
            json.decodeFromString<ChatRichPayload>(body)
        }.getOrElse {
            ChatRichPayload(type = ChatMediaTipo.TEXT.name, text = raw)
        }
    }

    fun estaOcultoPara(raw: String, usuarioId: String): Boolean {
        if (usuarioId.isBlank() || !raw.startsWith(PREFIX)) return false
        return usuarioId in decode(raw).hiddenFor
    }

    fun esEliminadoParaTodos(raw: String): Boolean {
        if (!raw.startsWith(PREFIX)) return false
        return decode(raw).tipo == ChatMediaTipo.DELETED
    }

    fun resumenLegible(raw: String): String {
        if (raw.isBlank()) return ""
        val p = decode(raw)
        return when (p.tipo) {
            ChatMediaTipo.TEXT -> p.text
            ChatMediaTipo.IMAGE -> if (p.text.isNotBlank()) "Foto: ${p.text}" else "Foto"
            ChatMediaTipo.AUDIO -> "Nota de voz (${formatearDuracion(p.durationSec)})"
            ChatMediaTipo.DOCUMENT -> "Documento: ${p.fileName.ifBlank { "Archivo adjunto" }}"
            ChatMediaTipo.LOCATION -> {
                if (p.addressLabel.isNotBlank()) "Ubicación: ${p.addressLabel}"
                else "Ubicación compartida"
            }
            ChatMediaTipo.CONTACT -> "Contacto: ${p.contactName.ifBlank { p.contactPhone }}"
            ChatMediaTipo.DELETED -> "Se eliminó este mensaje"
        }
    }

    fun formatearDuracion(segundos: Int): String {
        val safe = segundos.coerceAtLeast(0)
        val min = safe / 60
        val seg = safe % 60
        return String.format(Locale.US, "%d:%02d", min, seg)
    }

    fun formatearTamanoArchivo(bytes: Long): String {
        if (bytes <= 0L) return ""
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
