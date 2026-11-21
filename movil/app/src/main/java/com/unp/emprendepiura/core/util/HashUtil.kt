package com.unp.emprendepiura.core.util

import java.security.MessageDigest

/*
 * Archivo: HashUtil.kt
 * Proposito: Hash criptografico SHA-256 consistente con los scripts SQL de Supabase.
 */
object HashUtil {

    private const val SALT_PREFIX = "UNP_MARKET_2026:"

    fun sha256Password(rawPassword: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((SALT_PREFIX + rawPassword).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(rawPassword: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        return sha256Password(rawPassword).equals(storedHash.trim(), ignoreCase = true) ||
            rawPassword == storedHash
    }
}
