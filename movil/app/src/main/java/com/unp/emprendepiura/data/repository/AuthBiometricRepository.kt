package com.unp.emprendepiura.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.unp.emprendepiura.core.network.SupaClient
import com.unp.emprendepiura.data.dto.UsuarioRowDto
import com.unp.emprendepiura.data.dto.UsuarioUpsertDto
import com.unp.emprendepiura.data.dto.toDomain
import com.unp.emprendepiura.domain.model.Usuario
import com.unp.emprendepiura.features.auth.biometric.FaceAnalyzerUtil
import com.unp.emprendepiura.features.auth.biometric.FaceComparisonEngine
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

/*
 * Archivo: AuthBiometricRepository.kt
 * Proposito: Repositorio unificado de autenticacion biometrica y usuarios del Marketplace
 *            contra public.usuarios en Supabase + cache local sincronizada.
 */
class AuthBiometricRepository {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TABLE_USUARIOS = "usuarios"
        private const val PREFS_NAME = "emprendepiura_auth_store"
        private const val KEY_LAST_DNI = "last_enrolled_dni"
    }

    suspend fun getAll(context: Context): List<Usuario> {
        val remoto = runCatching {
            SupaClient.client.from(TABLE_USUARIOS)
                .select()
                .decodeList<UsuarioRowDto>()
                .map { it.toDomain() }
        }

        if (remoto.isSuccess) {
            val lista = remoto.getOrThrow()
            sincronizarRespaldoLocal(context, lista)
            return lista.map { asegurarDescriptorActualizado(context, it) }
        }

        return leerTodosDeRespaldoLocal(context).map { asegurarDescriptorActualizado(context, it) }
    }

    suspend fun getByDni(context: Context, dni: String): Usuario? {
        val cleanDni = dni.trim()
        val remoto = runCatching {
            SupaClient.client.from(TABLE_USUARIOS)
                .select { filter { eq("dni", cleanDni) } }
                .decodeList<UsuarioRowDto>()
                .firstOrNull()
                ?.toDomain()
        }

        if (remoto.isSuccess) {
            val user = remoto.getOrThrow()
            if (user != null) {
                val actualizado = asegurarDescriptorActualizado(context, user)
                guardarEnRespaldoLocal(context, actualizado)
                return actualizado
            } else {
                eliminarDeRespaldoLocal(context, cleanDni)
                return null
            }
        }

        return leerDeRespaldoLocal(context, cleanDni)?.let {
            asegurarDescriptorActualizado(context, it)
        }
    }

    suspend fun insertOrUpdate(context: Context, data: UsuarioUpsertDto): Usuario {
        val cleanDni = data.dni.trim()
        val payload = data.copy(dni = cleanDni)

        val guardado = runCatching {
            SupaClient.client.from(TABLE_USUARIOS)
                .upsert(payload) {
                    onConflict = "dni"
                    select()
                }
                .decodeSingle<UsuarioRowDto>()
                .toDomain()
        }.getOrElse { error ->
            runCatching {
                SupaClient.client.from(TABLE_USUARIOS)
                    .update(payload) {
                        select()
                        filter { eq("dni", cleanDni) }
                    }
                    .decodeSingle<UsuarioRowDto>()
                    .toDomain()
            }.getOrElse {
                val msg = error.message.orEmpty().lowercase()
                if (msg.contains("usuarios_correo_key")) throw IllegalStateException("DUPLICATE_EMAIL")
                if (msg.contains("duplicate") || msg.contains("23505")) throw IllegalStateException("DUPLICATE_DNI")
                throw error
            }
        }

        guardarEnRespaldoLocal(context, guardado)
        recordarUltimoDni(context, guardado.dni)
        return guardado
    }

    suspend fun uploadFacePhoto(context: Context, dni: String, imageBytes: ByteArray): String {
        val cleanDni = dni.trim()
        val remotePath = "rostros/${cleanDni}_rostro.jpg"

        val localDir = File(context.filesDir, "rostros").apply { mkdirs() }
        val localFile = File(localDir, "${cleanDni}_rostro.jpg")
        runCatching { localFile.writeBytes(imageBytes) }

        val timestamp = System.currentTimeMillis()
        val urlRemota = runCatching {
            SupaClient.client.storage.from(SupaClient.BUCKET_ROSTROS)
                .upload(remotePath, imageBytes) { upsert = true }
            val basePublicUrl = SupaClient.client.storage.from(SupaClient.BUCKET_ROSTROS)
                .publicUrl(remotePath)
            "$basePublicUrl?v=$timestamp"
        }.getOrNull()

        return if (!urlRemota.isNullOrBlank()) urlRemota else "${localFile.absolutePath}?v=$timestamp"
    }

    fun obtenerUltimoDniRecordado(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_DNI, "").orEmpty()
    }

    fun recordarUltimoDni(context: Context, dni: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_DNI, dni.trim()).apply()
    }

    private fun eliminarDeRespaldoLocal(context: Context, dni: String) {
        val cleanDni = dni.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().remove("dni_$cleanDni")
        if (prefs.getString(KEY_LAST_DNI, null) == cleanDni) {
            editor.remove(KEY_LAST_DNI)
        }
        editor.apply()
    }

    private fun sincronizarRespaldoLocal(context: Context, remotos: List<Usuario>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val dnisRemotos = remotos.map { it.dni }.toSet()

        prefs.all.keys.filter { it.startsWith("dni_") }.forEach { key ->
            if (key.removePrefix("dni_") !in dnisRemotos) editor.remove(key)
        }

        val ultimoDni = prefs.getString(KEY_LAST_DNI, null)
        if (ultimoDni != null && ultimoDni !in dnisRemotos) editor.remove(KEY_LAST_DNI)

        remotos.forEach { usuario ->
            editor.putString("dni_${usuario.dni}", json.encodeToString(usuario.toRowDto()))
        }
        editor.apply()
    }

    private suspend fun asegurarDescriptorActualizado(context: Context, usuario: Usuario): Usuario {
        if (usuario.vectorFacial.isBlank() || FaceComparisonEngine.esDescriptorCompleto(usuario.vectorFacial)) {
            return usuario
        }
        val ruta = usuario.imagenFacial.trim()
        if (ruta.isBlank()) return usuario

        val bitmap = runCatching {
            val rutaSinQuery = ruta.substringBefore("?")
            val localCandidate = File(context.filesDir, "rostros/${usuario.dni}_rostro.jpg")
            when {
                ruta.startsWith("http", ignoreCase = true) -> URL(ruta).openStream().use {
                    BitmapFactory.decodeStream(it)
                }
                File(rutaSinQuery).exists() -> BitmapFactory.decodeFile(rutaSinQuery)
                localCandidate.exists() -> BitmapFactory.decodeFile(localCandidate.absolutePath)
                else -> null
            }
        }.getOrNull() ?: return usuario

        val nuevoVector = FaceAnalyzerUtil.extraerVectorDesdeBitmap(context, bitmap) ?: return usuario
        val actualizado = usuario.copy(vectorFacial = nuevoVector)
        guardarEnRespaldoLocal(context, actualizado)

        runCatching {
            SupaClient.client.from(TABLE_USUARIOS).update(
                mapOf("vector_facial" to nuevoVector)
            ) {
                filter { eq("dni", actualizado.dni) }
            }
        }
        return actualizado
    }

    private fun guardarEnRespaldoLocal(context: Context, usuario: Usuario) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("dni_${usuario.dni}", json.encodeToString(usuario.toRowDto())).apply()
    }

    private fun leerDeRespaldoLocal(context: Context, dni: String): Usuario? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("dni_$dni", null) ?: return null
        return runCatching { json.decodeFromString<UsuarioRowDto>(raw).toDomain() }.getOrNull()
    }

    private fun leerTodosDeRespaldoLocal(context: Context): List<Usuario> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.entries
            .filter { it.key.startsWith("dni_") }
            .mapNotNull { entry ->
                val raw = entry.value as? String ?: return@mapNotNull null
                runCatching { json.decodeFromString<UsuarioRowDto>(raw).toDomain() }.getOrNull()
            }
    }

    private fun Usuario.toRowDto() = UsuarioRowDto(
        id = id,
        dni = dni,
        nombre = nombre,
        correo = correo,
        telefono = telefono,
        passwordHash = passwordHash,
        rol = rol.name,
        zonaPiura = zonaPiura,
        huella = huella,
        imagenFacial = imagenFacial,
        vectorFacial = vectorFacial,
        avatarUrl = avatarUrl,
        activo = activo
    )
}
