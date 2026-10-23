package com.unp.registroestudiantes.persona.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Archivo: PersonaActionButtons.kt
 * Proposito: Barra de acciones fiel al boceto original con 3 botones principales alineados.
 */

@Composable
fun ModoChip(
    modoEdicion: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (modoEdicion)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (modoEdicion) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (modoEdicion)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (modoEdicion) "Modo Edición" else "Nuevo Registro",
                style = MaterialTheme.typography.labelSmall,
                color = if (modoEdicion)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PersonaActionButtons(
    saving: Boolean,
    modoEdicion: Boolean,
    onNuevo: () -> Unit,
    onGuardar: () -> Unit,
    onActualizar: () -> Unit,
    onEliminarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fila inferior de 3 botones principales segun el boceto: [Nuevo] [Guardar/Actualizar] [Eliminar]
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Boton 1: Nuevo
        OutlinedButton(
            onClick = onNuevo,
            enabled = !saving,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Nuevo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Boton 2: Guardar (si es nuevo) o Actualizar (si esta en edicion)
        Button(
            onClick = if (modoEdicion) onActualizar else onGuardar,
            enabled = !saving,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            modifier = Modifier.weight(1.3f)
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = if (modoEdicion) Icons.Default.Edit else Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (modoEdicion) "Actualizar" else "Guardar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Boton 3: Eliminar (habilitado solo en modo edicion)
        Button(
            onClick = onEliminarClick,
            enabled = !saving && modoEdicion,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Eliminar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
