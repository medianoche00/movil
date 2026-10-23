package com.unp.registroestudiantes.persona.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Archivo: CameraUtil.kt
 * Proposito: Utilidad para la creacion de archivos temporales seguros para captura de fotos.
 */
object CameraUtil {

    /**
     * Genera un archivo JPEG temporal en el almacenamiento privado de la aplicacion
     * y retorna su URI segura mediante FileProvider.
     */
    fun crearArchivoFotoTemporal(context: Context): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("PERSONA_${timestamp}_", ".jpg", storageDir)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }
}
