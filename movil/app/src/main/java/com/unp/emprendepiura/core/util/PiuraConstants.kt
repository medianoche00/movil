package com.unp.emprendepiura.core.util

import java.util.Locale

/*
 * Archivo: PiuraConstants.kt
 * Proposito: Zonas locales, puntos de encuentro frecuentes y utilidades de formato para Piura y UNP.
 */
object PiuraConstants {

    val ZONAS_PIURA = listOf(
        "Campus UNP",
        "Castilla",
        "Piura Centro",
        "Veintiséis de Octubre",
        "Miraflores / Los Ejidos"
    )

    val PUNTOS_ENCUENTRO_UNP = listOf(
        "Explanada / Comedor UNP",
        "Puerta Principal UNP",
        "Facultad de Ingeniería Industrial",
        "Open Plaza Castilla",
        "Plaza de Armas de Piura",
        "Real Plaza Piura"
    )

    fun formatearSoles(monto: Double): String = String.format(Locale.US, "S/ %.2f", monto)
}
