package com.unp.emprendepiura.features.store

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Categoria
import com.unp.emprendepiura.domain.model.Producto

@Composable
fun ProductoEditorDialog(
    productoExistente: Producto?,
    categorias: List<Categoria>,
    zonaDefecto: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (
        titulo: String,
        descripcion: String,
        precioTexto: String,
        stockTexto: String,
        categoriaId: String,
        zonaPiura: String,
        imagenUri: Uri?
    ) -> Unit
) {
    var titulo by remember { mutableStateOf(productoExistente?.titulo.orEmpty()) }
    var descripcion by remember { mutableStateOf(productoExistente?.descripcion.orEmpty()) }
    var precio by remember { mutableStateOf(productoExistente?.precio?.toString().orEmpty()) }
    var stock by remember { mutableStateOf((productoExistente?.stock ?: 5).toString()) }
    var categoriaId by remember {
        mutableStateOf(productoExistente?.categoriaId ?: categorias.firstOrNull()?.id.orEmpty())
    }
    var zonaPiura by remember {
        mutableStateOf(productoExistente?.zonaPiura ?: zonaDefecto.ifBlank { PiuraConstants.ZONAS_PIURA.first() })
    }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imagenUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (productoExistente == null) "Publicar Producto" else "Editar Producto",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val previewModel: Any? = imagenUri ?: productoExistente?.imagenUrl?.takeIf { it.isNotBlank() }
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null)
                            Text("Toca para elegir foto de galería", fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = precio,
                        onValueChange = { precio = it },
                        label = { Text("Precio S/ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Text("Categoría", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categorias.forEach { cat ->
                        FilterChip(
                            selected = categoriaId == cat.id,
                            onClick = { categoriaId = cat.id },
                            label = { Text(cat.nombre, fontSize = 11.5.sp) }
                        )
                    }
                }

                Text("Zona Piura / UNP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PiuraConstants.ZONAS_PIURA.forEach { z ->
                        FilterChip(
                            selected = zonaPiura == z,
                            onClick = { zonaPiura = z },
                            label = { Text(z, fontSize = 11.5.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { onGuardar(titulo, descripcion, precio, stock, categoriaId, zonaPiura, imagenUri) },
                        enabled = !saving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
