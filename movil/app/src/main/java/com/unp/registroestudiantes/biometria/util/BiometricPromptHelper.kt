package com.unp.registroestudiantes.biometria.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

/*
 * Archivo: BiometricPromptHelper.kt
 * Proposito: Integracion con el lector fisico de huella dactilar de Android mediante BiometricPrompt
 *            y generacion del token criptografico de vinculacion para PostgreSQL.
 */
object BiometricPromptHelper {

    /**
     * Despliega el dialogo oficial del sistema operativo para leer la huella dactilar.
     */
    fun autenticarHuella(
        context: Context,
        titulo: String,
        subtitulo: String,
        dni: String,
        onExito: (tokenHuella: String) -> Unit,
        onError: (mensaje: String) -> Unit
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            onError("No se pudo inicializar el gestor biométrico en la actividad actual.")
            return
        }

        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onError("Este dispositivo no cuenta con sensor físico de huella dactilar.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onError("El sensor de huella no está disponible en este momento.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("No hay ninguna huella registrada en los Ajustes de seguridad del teléfono.")
                return
            }
            else -> Unit
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val token = generarTokenCriptografico(dni)
                onExito(token)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onError("Lectura en pausa. Presione Reintentar para volver a escanear su huella.")
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("La huella dactilar no fue reconocida. Intente nuevamente.")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText("Cancelar")
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    /**
     * Genera una firma criptografica SHA-256 unica asociada al DNI y marca de tiempo tras validar el sensor fisico.
     */
    fun generarTokenCriptografico(dni: String): String {
        val cleanDni = dni.trim().ifEmpty { "00000000" }
        val timestamp = System.currentTimeMillis()
        val payload = "UNP_BIOMETRIC_FP_${cleanDni}_${timestamp}"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        val hex = digest.take(12).joinToString("") { "%02X".format(it) }
        return "FP_${cleanDni}_$hex"
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }
}
