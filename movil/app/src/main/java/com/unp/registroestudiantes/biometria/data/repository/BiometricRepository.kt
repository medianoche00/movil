package com.unp.registroestudiantes.biometria.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometrico
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometricoRow
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometricoWrite
import com.unp.registroestudiantes.biometria.data.model.toDomain
import com.unp.registroestudiantes.biometria.domain.FaceComparisonEngine
import com.unp.registroestudiantes.biometria.util.FaceAnalyzerUtil
import com.unp.registroestudiantes.data.Supa
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

/*
 * Archivo: BiometricRepository.kt
 * Proposito: Repositorio para la persistencia de usuarios biometricos contra Supabase / PostgreSQL.
 *            Mantiene la base de datos remota como unica fuente de la verdad y sincroniza
 *            la cache local (eliminando registros que hayan sido borrados en Supabase).
 */
class BiometricRepository {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TABLE_NAME = "usuarios_biometricos"
        private const val PREFS_NAME = "biometric_users_store"
        private const val KEY_LAST_DNI = "last_enrolled_dni"
    }

    /**
     * Obtiene todos los usuarios registrados. Si la consulta remota responde con exito,
     * Supabase es la fuente de la verdad y se sincroniza la cache local (borrando registros obsoletos).
     */
    suspend fun getAll(context: Context): List<UsuarioBiometrico> {
        val resultadoRemoto = runCatching {
            Supa.client.from(TABLE_NAME)
                .select()
                .decodeList<UsuarioBiometricoRow>()
                .map { it.toDomain() }
        }

        if (resultadoRemoto.isSuccess) {
            val remotos = resultadoRemoto.getOrThrow()
            sincronizarRespaldoLocal(context, remotos)
            return remotos.map { asegurarDescriptorActualizado(context, it) }
        }

        // Respaldo de solo lectura en caso de perdida de conexion a internet
        val locales = leerTodosDeRespaldoLocal(context)
        return locales.map { asegurarDescriptorActualizado(context, it) }
    }

    /**
     * Busca un usuario por DNI en Supabase. Si Supabase responde exitosamente pero no encuentra
     * el registro (por ejemplo, porque fue borrado en la base de datos), se elimina de la cache local
     * y se retorna null para que el usuario pueda registrarse desde cero.
     */
    suspend fun getByDni(context: Context, dni: String): UsuarioBiometrico? {
        val cleanDni = dni.trim()
        val resultadoRemoto = runCatching {
            Supa.client.from(TABLE_NAME)
                .select { filter { eq("dni", cleanDni) } }
                .decodeList<UsuarioBiometricoRow>()
                .firstOrNull()
                ?.toDomain()
        }

        if (resultadoRemoto.isSuccess) {
            val remoto = resultadoRemoto.getOrThrow()
            if (remoto != null) {
                val actualizado = asegurarDescriptorActualizado(context, remoto)
                guardarEnRespaldoLocal(context, actualizado)
                return actualizado
            } else {
                // El registro fue borrado de Supabase: limpiar cache local y el DNI recordado
                eliminarDeRespaldoLocal(context, cleanDni)
                return null
            }
        }

        // Solo si la consulta fallo por error de red / sin conexion, se recurre a la cache local
        return leerDeRespaldoLocal(context, cleanDni)?.let {
            asegurarDescriptorActualizado(context, it)
        }
    }

    /** Obtiene el usuario vinculado a la huella de este dispositivo validando que exista en Supabase. */
    suspend fun obtenerUsuarioParaHuella(context: Context): UsuarioBiometrico? {
        val ultimoDni = obtenerUltimoDniRecordado(context)
        if (ultimoDni.isNotBlank()) {
            getByDni(context, ultimoDni)?.let { return it }
        }
        return getAll(context).lastOrNull { it.huella.isNotBlank() }
    }

    /** Registra o actualiza un usuario en Supabase y sincroniza el almacenamiento local. */
    suspend fun insert(context: Context, data: UsuarioBiometricoWrite): UsuarioBiometrico {
        val guardadoRemoto = runCatching {
            Supa.client.from(TABLE_NAME)
                .upsert(data) { select() }
                .decodeSingle<UsuarioBiometricoRow>()
                .toDomain()
        }.getOrElse { error ->
            val msg = error.message.orEmpty().lowercase()
            if (msg.contains("duplicate") || msg.contains("23505") || msg.contains("unique")) {
                throw IllegalStateException("DUPLICATE_DNI")
            }
            throw error
        }

        guardarEnRespaldoLocal(context, guardadoRemoto)
        recordarUltimoDni(context, guardadoRemoto.dni)
        return guardadoRemoto
    }

    /** Sube la fotografia de reconocimiento facial a Supabase Storage y retorna su URL publica. */
    suspend fun uploadFacePhoto(context: Context, dni: String, imageBytes: ByteArray): String {
        val cleanDni = dni.trim()
        val remotePath = "biometria/${cleanDni}_rostro.jpg"

        val localDir = File(context.filesDir, "rostros").apply { mkdirs() }
        val localFile = File(localDir, "${cleanDni}_rostro.jpg")
        runCatching { localFile.writeBytes(imageBytes) }

        val urlRemota = runCatching {
            Supa.client.storage.from(Supa.PERSONAS_PHOTOS_BUCKET)
                .upload(remotePath, imageBytes) { upsert = true }
            Supa.client.storage.from(Supa.PERSONAS_PHOTOS_BUCKET)
                .publicUrl(remotePath)
        }.getOrNull()

        return if (!urlRemota.isNullOrBlank()) urlRemota else localFile.absolutePath
    }

    fun obtenerUltimoDniRecordado(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_DNI, "").orEmpty()
    }

    fun recordarUltimoDni(context: Context, dni: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_DNI, dni.trim()).apply()
    }

    fun eliminarDeRespaldoLocal(context: Context, dni: String) {
        val cleanDni = dni.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().remove("dni_$cleanDni")
        val ultimoDni = prefs.getString(KEY_LAST_DNI, null)
        if (ultimoDni == cleanDni) {
            editor.remove(KEY_LAST_DNI)
        }
        editor.apply()
    }

    private fun sincronizarRespaldoLocal(context: Context, remotos: List<UsuarioBiometrico>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val dnisRemotos = remotos.map { it.dni }.toSet()

        // Eliminar del respaldo local cualquier registro que haya sido eliminado de Supabase
        prefs.all.keys.filter { it.startsWith("dni_") }.forEach { key ->
            val dniEnKey = key.removePrefix("dni_")
            if (dniEnKey !in dnisRemotos) {
                editor.remove(key)
            }
        }

        val ultimoDni = prefs.getString(KEY_LAST_DNI, null)
        if (ultimoDni != null && ultimoDni !in dnisRemotos) {
            editor.remove(KEY_LAST_DNI)
        }

        // Guardar los registros activos de Supabase
        remotos.forEach { usuario ->
            val row = UsuarioBiometricoRow(
                dni = usuario.dni,
                nombre = usuario.nombre,
                contrasena = usuario.contrasena,
                huella = usuario.huella,
                imagenFacial = usuario.imagenFacial,
                vectorFacial = usuario.vectorFacial
            )
            editor.putString("dni_${usuario.dni}", json.encodeToString(row))
        }
        editor.apply()
    }

    /**
     * Si un usuario fue registrado con una version anterior del vector facial (< 232 dimensiones),
     * recalcula el descriptor de 232 dimensiones a partir de su fotografia almacenada.
     */
    private suspend fun asegurarDescriptorActualizado(
        context: Context,
        usuario: UsuarioBiometrico
    ): UsuarioBiometrico {
        if (FaceComparisonEngine.esDescriptorCompleto(usuario.vectorFacial)) {
            return usuario
        }
        val ruta = usuario.imagenFacial.trim()
        if (ruta.isBlank()) return usuario

        val bitmap = runCatching {
            val localCandidate = File(context.filesDir, "rostros/${usuario.dni}_rostro.jpg")
            when {
                localCandidate.exists() -> BitmapFactory.decodeFile(localCandidate.absolutePath)
                ruta.startsWith("http", ignoreCase = true) -> URL(ruta).openStream().use {
                    BitmapFactory.decodeStream(it)
                }
                File(ruta).exists() -> BitmapFactory.decodeFile(ruta)
                else -> null
            }
        }.getOrNull() ?: return usuario

        val nuevoVector = FaceAnalyzerUtil.extraerVectorDesdeBitmap(bitmap) ?: return usuario
        val actualizado = usuario.copy(vectorFacial = nuevoVector)
        guardarEnRespaldoLocal(context, actualizado)

        runCatching {
            val payload = UsuarioBiometricoWrite(
                dni = actualizado.dni,
                nombre = actualizado.nombre,
                contrasena = actualizado.contrasena,
                huella = actualizado.huella,
                imagenFacial = actualizado.imagenFacial,
                vectorFacial = nuevoVector
            )
            Supa.client.from(TABLE_NAME).upsert(payload)
        }
        return actualizado
    }

    private fun guardarEnRespaldoLocal(context: Context, usuario: UsuarioBiometrico) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val row = UsuarioBiometricoRow(
            dni = usuario.dni,
            nombre = usuario.nombre,
            contrasena = usuario.contrasena,
            huella = usuario.huella,
            imagenFacial = usuario.imagenFacial,
            vectorFacial = usuario.vectorFacial
        )
        prefs.edit().putString("dni_${usuario.dni}", json.encodeToString(row)).apply()
    }

    private fun leerDeRespaldoLocal(context: Context, dni: String): UsuarioBiometrico? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("dni_$dni", null) ?: return null
        return runCatching {
            json.decodeFromString<UsuarioBiometricoRow>(raw).toDomain()
        }.getOrNull()
    }

    private fun leerTodosDeRespaldoLocal(context: Context): List<UsuarioBiometrico> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.entries
            .filter { it.key.startsWith("dni_") }
            .mapNotNull { entry ->
                val raw = entry.value as? String ?: return@mapNotNull null
                runCatching { json.decodeFromString<UsuarioBiometricoRow>(raw).toDomain() }.getOrNull()
            }
    }
}
