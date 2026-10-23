package com.unp.registroestudiantes.persona.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.unp.registroestudiantes.persona.data.model.Persona

/*
 * Archivo: PersonaDeleteDialog.kt
 * Proposito: Dialogo modal para confirmar la eliminacion definitiva de un registro.
 */
@Composable
fun PersonaDeleteDialog(
    persona: Persona?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (persona == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Confirmar eliminación", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "¿Está seguro de que desea eliminar a \"${persona.nombreCompleto}\" " +
                "con DNI ${persona.dni}?\n\nEsta acción no se puede deshacer."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
