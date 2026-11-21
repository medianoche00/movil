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
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitarTiendaScreen(
    categorias: List<Categoria>,
    telefonoInicial: String,
    zonaInicial: String,
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
        mutableStateOf(zonaInicial.ifBlank { PiuraConstants.ZONAS_PIURA.first() })
    }
    var puntoEncuentro by remember { mutableStateOf(PiuraConstants.PUNTOS_ENCUENTRO_UNP.first()) }
    var telefono by remember { mutableStateOf(telefonoInicial) }
    var yape by remember { mutableStateOf(telefonoInicial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Abrir mi Emprendimiento", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Completa los datos de tu emprendimiento. Un administrador aprobará tu tienda para que puedas publicar productos.",
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
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Categoría principal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val opcionesCat = if (categorias.isNotEmpty()) categorias.map { it.nombre } else listOf("Alimentos y Postres", "Servicios UNP")
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
                    FilterChip(selected = zonaPiura == z, onClick = { zonaPiura = z }, label = { Text(z, fontSize = 12.sp) })
                }
            }

            OutlinedTextField(
                value = puntoEncuentro,
                onValueChange = { puntoEncuentro = it },
                label = { Text("Punto de entrega habitual *") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it.filter { c -> c.isDigit() }.take(9) },
                label = { Text("Celular de contacto *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = yape,
                onValueChange = { yape = it.filter { c -> c.isDigit() }.take(9) },
                label = { Text("Número Yape / Plin (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    onEnviarSolicitud(nombreTienda, descripcion, categoriaPrincipal, zonaPiura, puntoEncuentro, telefono, yape)
                },
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar Solicitud de Emprendimiento", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
