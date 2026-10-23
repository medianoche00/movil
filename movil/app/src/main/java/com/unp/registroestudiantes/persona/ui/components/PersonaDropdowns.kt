package com.unp.registroestudiantes.persona.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unp.registroestudiantes.persona.data.model.Profesion

/*
 * Archivo: PersonaDropdowns.kt
 * Proposito: Selector desplegable para catalogo de profesiones con estilo redondeado moderno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfesionDropdown(
    profesiones: List<Profesion>,
    selectedCodigo: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = profesiones.firstOrNull { it.codigoProfesion == selectedCodigo }?.nombre
        ?: "Seleccione una profesión"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Profesión *") },
            leadingIcon = { Icon(Icons.Default.WorkOutline, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            profesiones.forEach { prof ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(prof.nombre, fontWeight = FontWeight.Medium)
                            Text(
                                prof.codigoProfesion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(prof.codigoProfesion)
                        expanded = false
                    },
                    leadingIcon = if (prof.codigoProfesion == selectedCodigo) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}
