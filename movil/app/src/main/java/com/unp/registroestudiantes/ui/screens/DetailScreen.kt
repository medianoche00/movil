package com.unp.registroestudiantes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    student: Student,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(student.name) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } },
            actions = {
                IconButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "${student.name} — ${student.career}\n\n${student.review}")
                    }
                    ctx.startActivity(android.content.Intent.createChooser(intent, "Compartir reseña"))
                }) { Icon(Icons.Default.Share, "Compartir") }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Eliminar") }
            }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScrollFix().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(140.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (student.photoUrl != null) {
                    AsyncImage(student.photoUrl, student.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(student.name, style = MaterialTheme.typography.headlineSmall)
            Text("${student.career} · ${student.age} años", color = MaterialTheme.colorScheme.onSurfaceVariant)
            student.email?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            Spacer(Modifier.height(20.dp))
            SectionCard(title = "Reseña") { Text(student.review.ifBlank { "Sin reseña disponible." }, style = MaterialTheme.typography.bodyMedium) }

            Spacer(Modifier.height(14.dp))
            SectionCard(title = "Aptitudes detectadas") {
                if (student.skills.isEmpty()) {
                    Text("No se detectaron aptitudes en el CV.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowSkills(student.skills.map { it.name })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar a ${student.name}?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun FlowSkills(names: List<String>) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEachIndexed { i, n ->
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(i * 60L); shown = true }
            AnimatedVisibility(visible = shown, enter = scaleIn(tween(220)) + fadeIn(tween(220))) {
                AssistChip(onClick = {}, label = { Text(n) })
            }
        }
    }
}

@Composable
private fun Modifier.verticalScrollFix(): Modifier {
    val scroll = androidx.compose.foundation.rememberScrollState()
    return this.then(androidx.compose.foundation.verticalScroll(scroll))
}
