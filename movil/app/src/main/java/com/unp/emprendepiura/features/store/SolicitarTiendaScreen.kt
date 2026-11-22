package com.unp.emprendepiura.features.store

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria

/*
 * Archivo: SolicitarTiendaScreen.kt
 * Proposito: Formulario limpio de solicitud de emprendimiento. Al salir o retroceder
 *            se descartan todos los campos ingresados para no retener datos previos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitarTiendaScreen(
    categorias: List<Categoria>,
    saving: Boolean,
    onEnviarSolicitud: (
        nombreTienda: String,
        descripcion: String,
        categoriaPrincipal: String,
        zonaPiura: String,
        puntoEncuentro: String,
        telefono: String,
        numeroYapePlin: String
    ) -> Unit,
    onVolver: () -> Unit
) {
    var nombreTienda by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaPrincipal by remember {
        mutableStateOf(categorias.firstOrNull()?.nombre ?: "Alimentos y Postres")
    }
    var zonaPiura by remember {
        mutableStateOf(PiuraConstants.ZONAS_PIURA.first())
    }
    var puntoEncuentro by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var yape by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Solicitud de Emprendimiento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Completa los datos de tu emprendimiento. Un administrador revisará tu solicitud para activar tu tienda.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nombreTienda,
                onValueChange = { nombreTienda = it },
                label = { Text("Nombre del Emprendimiento *") },
                placeholder = { Text("Ej. Postres UNP, Artesanías Grau") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("¿Qué ofreces?") },
                placeholder = { Text("Describe brevemente tus productos o servicios") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Categoría principal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val opcionesCat = if (categorias.isNotEmpty()) {
                    categorias.map { it.nombre }
                } else {
                    listOf("Alimentos y Postres", "Servicios UNP")
                }
                opcionesCat.forEach { cat ->
                    FilterChip(
                        selected = categoriaPrincipal == cat,
                        onClick = { categoriaPrincipal = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Text("Zona principal en Piura / UNP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PiuraConstants.ZONAS_PIURA.forEach { z ->
                    FilterChip(
                        selected = zonaPiura == z,
                        onClick = { zonaPiura = z },
                        label = { Text(z, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = puntoEncuentro,
                onValueChange = { puntoEncuentro = it },
                label = { Text("Punto de entrega habitual *") },
                placeholder = { Text("Ej. Puerta Principal UNP, Comedor Universitario") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(9) },
                label = { Text("Celular de contacto *") },
                placeholder = { Text("Ej. 969123456") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = yape,
                onValueChange = { yape = it.filter { c -> c.isDigit() }.take(9) },
                label = { Text("Número Yape / Plin (opcional)") },
                placeholder = { Text("Si se deja vacío, se usará el celular de contacto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            val sendPress = rememberPressInteraction()
            Button(
                onClick = {
                    onEnviarSolicitud(
                        nombreTienda,
                        descripcion,
                        categoriaPrincipal,
                        zonaPiura,
                        puntoEncuentro,
                        telefono,
                        yape
                    )
                },
                enabled = !saving,
                interactionSource = sendPress,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .pressScale(sendPress)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar solicitud", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }
            }
        }
    }
}
