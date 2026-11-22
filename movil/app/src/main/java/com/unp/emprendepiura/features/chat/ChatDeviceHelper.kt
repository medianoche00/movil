package com.unp.emprendepiura.features.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/*
 * Archivo: ChatDeviceHelper.kt
 * Proposito: Integracion nativa Android para adjuntos de chat: camara via FileProvider,
 *            lectura de documentos (Word/Excel/PDF), seleccion de contactos y ubicacion GPS.
 */
data class DocumentoLeido(
    val nombre: String,
    val mimeType: String,
    val extension: String,
    val bytes: ByteArray
)

data class UbicacionCapturada(
    val lat: Double,
    val lng: Double,
    val direccionLegible: String
)

object ChatDeviceHelper {

    private const val MAX_DOC_BYTES = 15 * 1024 * 1024 // 15 MB

    fun crearUriFotoTemporalCamara(context: Context): Uri {
        val dir = File(context.cacheDir, "chat_camera").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    fun leerDocumentoDesdeUri(context: Context, uri: Uri): Result<DocumentoLeido> {
        return runCatching {
            val resolver = context.contentResolver
            var nombre = "documento"
            var tamano = -1L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idxNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idxNombre >= 0) {
                        val n = cursor.getString(idxNombre)
                        if (!n.isNullOrBlank()) nombre = n
                    }
                    val idxSize = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idxSize >= 0 && !cursor.isNull(idxSize)) {
                        tamano = cursor.getLong(idxSize)
                    }
                }
            }

            if (tamano > MAX_DOC_BYTES) {
                throw IllegalArgumentException("El documento supera el límite permitido de 15 MB.")
            }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("No se pudo leer el archivo seleccionado.")

            if (bytes.isEmpty()) {
                throw IllegalArgumentException("El archivo seleccionado está vacío.")
            }
            if (bytes.size > MAX_DOC_BYTES) {
                throw IllegalArgumentException("El documento supera el límite permitido de 15 MB.")
            }

            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            val extDesdeNombre = nombre.substringAfterLast('.', "").lowercase(Locale.US)
                .filter { it.isLetterOrDigit() }
                .take(8)
            val extDesdeMime = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?.lowercase(Locale.US)
                ?.filter { it.isLetterOrDigit() }
                ?.take(8)
                .orEmpty()
            val extension = extDesdeNombre.ifBlank { extDesdeMime.ifBlank { "bin" } }

            DocumentoLeido(
                nombre = nombre,
                mimeType = mimeType,
                extension = extension,
                bytes = bytes
            )
        }
    }

    fun extraerContactoDesdeUri(context: Context, contactUri: Uri): Pair<String, String>? {
        val resolver = context.contentResolver
        var contactId: String? = null
        var nombre = ""
        var telefono = ""

        runCatching {
            resolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIdx >= 0) contactId = c.getString(idIdx)
                    if (nameIdx >= 0) nombre = c.getString(nameIdx).orEmpty()
                }
            }

            if (!contactId.isNullOrBlank()) {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { pc ->
                    if (pc.moveToFirst()) {
                        val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx >= 0) telefono = pc.getString(numIdx).orEmpty().trim()
                    }
                }
            }
        }

        if (nombre.isBlank() && telefono.isBlank()) return null
        return nombre.ifBlank { "Contacto" } to telefono.ifBlank { "Sin número registrado" }
    }

    fun tienePermisoUbicacion(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(context: Context): UbicacionCapturada? = withContext(Dispatchers.IO) {
        if (!tienePermisoUbicacion(context)) return@withContext null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val proveedoresCandidatos = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }.filter { prov -> runCatching { manager.isProviderEnabled(prov) }.getOrDefault(false) }

        var location: Location? = null

        // 1. Intentar ubicacion fresca en Android 11+ (timeout breve para no bloquear en interiores)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provActivo = proveedoresCandidatos.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
            if (provActivo != null) {
                location = withTimeoutOrNull(4_500L) {
                    suspendCancellableCoroutine { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        runCatching {
                            manager.getCurrentLocation(
                                provActivo,
                                signal,
                                ContextCompat.getMainExecutor(context)
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            }
        }

        // 2. Respaldo: ultima ubicacion conocida mas reciente y precisa
        if (location == null) {
            val todosProveedores = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
            location = todosProveedores
                .mapNotNull { prov -> runCatching { manager.getLastKnownLocation(prov) }.getOrNull() }
                .maxByOrNull { it.time }
        }

        val finalLoc = location ?: return@withContext null
        val direccion = resolverDireccionLegible(context, finalLoc.latitude, finalLoc.longitude)

        UbicacionCapturada(
            lat = finalLoc.latitude,
            lng = finalLoc.longitude,
            direccionLegible = direccion
        )
    }

    @Suppress("DEPRECATION")
    private fun resolverDireccionLegible(context: Context, lat: Double, lng: Double): String {
        val geocoded = runCatching {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            val lista = geocoder.getFromLocation(lat, lng, 1)
            val addr = lista?.firstOrNull()
            if (addr != null) {
                val linea = addr.getAddressLine(0)
                if (!linea.isNullOrBlank()) {
                    linea
                } else {
                    listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality)
                        .joinToString(", ")
                }
            } else ""
        }.getOrDefault("")

        return geocoded.ifBlank {
            String.format(Locale.US, "Piura (%.5f, %.5f)", lat, lng)
        }
    }

    fun abrirMapaUbicacion(context: Context, lat: Double, lng: Double, etiqueta: String) {
        val labelEnc = Uri.encode(etiqueta.ifBlank { "Ubicación compartida" })
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($labelEnc)")
        val intentMapa = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val abierto = runCatching { context.startActivity(intentMapa) }.isSuccess
        if (!abierto) {
            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, webUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
            }.onFailure {
                Toast.makeText(context, "No se encontró una aplicación de mapas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun abrirUrlExterna(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "No se pudo abrir el archivo.", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirMarcadorTelefonico(context: Context, telefono: String) {
        val limpio = telefono.filter { it.isDigit() || it == '+' }
        if (limpio.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$limpio")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "No se pudo abrir el marcador.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copiarAlPortapapeles(context: Context, label: String, valor: String) {
        if (valor.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, valor))
        Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }
}
