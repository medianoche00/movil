package com.unp.registroestudiantes.persona.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.registroestudiantes.persona.data.model.Profesion

/*
 * Archivo: PersonaFormSection.kt
 * Proposito: Campos del formulario ordenados fielmente segun el boceto original del Trabajo N°03.
 */

@Composable
fun PersonaDniSearchRow(
    dni: String,
    loading: Boolean,
    onDniChange: (String) -> Unit,
    onBuscar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = dni,
            onValueChange = onDniChange,
            label = { Text("DNI *") },
            placeholder = { Text("8 dígitos") },
            leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search
            ),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onBuscar,
            enabled = !loading && dni.length == 8,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Search, "Buscar", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Buscar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PersonaFormFields(
    dni: String,
    nombres: String,
    apellidos: String,
    direccion: String,
    codigoProfesion: String,
    celular: String,
    correoElectronico: String,
    profesiones: List<Profesion>,
    loadingProfesiones: Boolean,
    loadingBuscar: Boolean,
    onDniChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onNombresChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onDireccionChange: (String) -> Unit,
    onProfesionSelect: (String) -> Unit,
    onCelularChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. DNI + Boton Buscar (segun boceto)
        PersonaDniSearchRow(
            dni = dni,
            loading = loadingBuscar,
            onDniChange = onDniChange,
            onBuscar = onBuscar
        )

        // 2. Nombres (segun boceto)
        OutlinedTextField(
            value = nombres,
            onValueChange = onNombresChange,
            label = { Text("Nombres *") },
            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 3. Apellidos (segun boceto)
        OutlinedTextField(
            value = apellidos,
            onValueChange = onApellidosChange,
            label = { Text("Apellidos *") },
            leadingIcon = { Icon(Icons.Default.PersonOutline, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 4. Direccion (segun boceto)
        OutlinedTextField(
            value = direccion,
            onValueChange = onDireccionChange,
            label = { Text("Dirección *") },
            leadingIcon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 5. Profesion (segun boceto, colocado entre Direccion y Celular)
        if (loadingProfesiones) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Cargando catálogo de profesiones...", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            ProfesionDropdown(
                profesiones = profesiones,
                selectedCodigo = codigoProfesion,
                onSelect = onProfesionSelect
            )
        }

        // 6. Celular (segun boceto)
        OutlinedTextField(
            value = celular,
            onValueChange = onCelularChange,
            label = { Text("Celular *") },
            placeholder = { Text("9 dígitos") },
            leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 7. Correo Electronico
        OutlinedTextField(
            value = correoElectronico,
            onValueChange = onCorreoChange,
            label = { Text("Correo Electrónico *") },
            leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
