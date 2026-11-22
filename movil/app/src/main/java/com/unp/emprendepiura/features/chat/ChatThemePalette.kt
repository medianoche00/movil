package com.unp.emprendepiura.features.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Archivo: ChatThemePalette.kt
 * Proposito: Paleta de colores original de EmprendePiura para mensajeria (Porcelana & Cobalto en
 *            tema claro; Obsidiana & Zafiro en tema oscuro), respetando el tema manual de la app
 *            y conservando el doble visto azul representativo de lectura.
 */
@Immutable
data class EmprendeChatPalette(
    val isDark: Boolean,
    val canvasBackground: Color,
    val barSurface: Color,
    val barDivider: Color,
    val accentPrimary: Color,
    val accentWarmGold: Color,
    val onlineDot: Color,
    val outgoingBubbleStart: Color,
    val outgoingBubbleEnd: Color,
    val outgoingBubbleBorder: Color,
    val outgoingText: Color,
    val outgoingMeta: Color,
    val incomingBubble: Color,
    val incomingBubbleBorder: Color,
    val incomingText: Color,
    val incomingMeta: Color,
    val inputSurface: Color,
    val inputBorder: Color,
    val inputPlaceholder: Color,
    val dateBadgeBg: Color,
    val dateBadgeText: Color,
    val avatarBg: Color,
    val avatarText: Color,
    val checkEnviado: Color,
    val checkLeidoBlue: Color,
    val unreadBadgeBg: Color,
    val unreadBadgeText: Color
)

@Composable
fun rememberEmprendeChatPalette(darkTheme: Boolean): EmprendeChatPalette {
    return remember(darkTheme) {
        if (darkTheme) {
            EmprendeChatPalette(
                isDark = true,
                canvasBackground = Color(0xFF0B101B),
                barSurface = Color(0xFF121A2A),
                barDivider = Color(0xFF1E2B42),
                accentPrimary = Color(0xFF3B82F6),
                accentWarmGold = Color(0xFFFBBF24),
                onlineDot = Color(0xFF10B981),
                outgoingBubbleStart = Color(0xFF1E3A6E),
                outgoingBubbleEnd = Color(0xFF172D57),
                outgoingBubbleBorder = Color(0xFF2E5294),
                outgoingText = Color(0xFFF8FAFC),
                outgoingMeta = Color(0xFF94A3B8),
                incomingBubble = Color(0xFF182235),
                incomingBubbleBorder = Color(0xFF24334C),
                incomingText = Color(0xFFF1F5F9),
                incomingMeta = Color(0xFF8B9BB4),
                inputSurface = Color(0xFF192438),
                inputBorder = Color(0xFF283854),
                inputPlaceholder = Color(0xFF8191AA),
                dateBadgeBg = Color(0xFF182336),
                dateBadgeText = Color(0xFF94A3B8),
                avatarBg = Color(0xFF1C2D4F),
                avatarText = Color(0xFF60A5FA),
                checkEnviado = Color(0xFF7B8FA8),
                checkLeidoBlue = Color(0xFF38BDF8),
                unreadBadgeBg = Color(0xFF3B82F6),
                unreadBadgeText = Color.White
            )
        } else {
            EmprendeChatPalette(
                isDark = false,
                canvasBackground = Color(0xFFF3F6FB),
                barSurface = Color(0xFFFFFFFF),
                barDivider = Color(0xFFE2E8F0),
                accentPrimary = Color(0xFF2563EB),
                accentWarmGold = Color(0xFFD97706),
                onlineDot = Color(0xFF10B981),
                outgoingBubbleStart = Color(0xFFE3EDFF),
                outgoingBubbleEnd = Color(0xFFD8E6FF),
                outgoingBubbleBorder = Color(0xFFC2D7FF),
                outgoingText = Color(0xFF0F172A),
                outgoingMeta = Color(0xFF475569),
                incomingBubble = Color(0xFFFFFFFF),
                incomingBubbleBorder = Color(0xFFE5ECF6),
                incomingText = Color(0xFF0F172A),
                incomingMeta = Color(0xFF64748B),
                inputSurface = Color(0xFFF1F5FA),
                inputBorder = Color(0xFFDCE4F0),
                inputPlaceholder = Color(0xFF7C8B9E),
                dateBadgeBg = Color(0xFFE7EEF8),
                dateBadgeText = Color(0xFF475569),
                avatarBg = Color(0xFFE6EFFE),
                avatarText = Color(0xFF1D4ED8),
                checkEnviado = Color(0xFF8A99AF),
                checkLeidoBlue = Color(0xFF0284C7),
                unreadBadgeBg = Color(0xFF2563EB),
                unreadBadgeText = Color.White
            )
        }
    }
}

private val horaLocalFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val fechaCortaFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-PE"))

fun formatearHoraLocalChat(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return ""
    return parsearZonaLocal(isoTimestamp)?.format(horaLocalFormatter)
        ?: isoTimestamp.drop(11).take(5)
}

fun formatearEtiquetaDiaChat(isoTimestamp: String): String {
    val zoned = parsearZonaLocal(isoTimestamp) ?: return "Hoy"
    val fecha = zoned.toLocalDate()
    val hoy = LocalDate.now(ZoneId.systemDefault())
    return when (fecha) {
        hoy -> "Hoy"
        hoy.minusDays(1) -> "Ayer"
        else -> fecha.format(fechaCortaFormatter)
    }
}

fun claveDiaChat(isoTimestamp: String): String {
    return parsearZonaLocal(isoTimestamp)?.toLocalDate()?.toString()
        ?: isoTimestamp.take(10)
}

private fun parsearZonaLocal(iso: String) = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
}.recoverCatching {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
}.getOrNull()
