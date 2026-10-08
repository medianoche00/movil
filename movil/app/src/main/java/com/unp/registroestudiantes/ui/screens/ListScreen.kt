package com.unp.registroestudiantes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Student
import com.unp.registroestudiantes.ui.theme.pressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    students: List<Student>,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Student) -> Unit,
    onOpenContacts: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(students, query) {
        if (query.isBlank()) students
        else students.filter { it.name.contains(query, true) || it.career.contains(query, true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Estudiantes")
                        Text(
                            "${students.size} registrados",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenContacts) {
                        Icon(
                            Icons.Default.ContactPhone,
                            contentDescription = "Agenda de Contactos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, modifier = Modifier.pressScale()) {
                Icon(Icons.Default.Add, contentDescription = "Agregar estudiante")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar por nombre o carrera") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                loadError != null -> ErrorState(loadError, onRetry)
                filtered.isEmpty() && query.isBlank() -> EmptyState(onAdd)
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Sin resultados para \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filtered) { index, s -> StaggeredIn(index) { StudentCard(s) { onOpen(s) } } }
                }
            }
        }
    }
}

@Composable
private fun StaggeredIn(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(minOf(index, 8) * 40L); shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 }
    ) { content() }
}

@Composable
private fun StudentCard(s: Student, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().pressScale()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer)) {
                if (s.photoUrl != null) {
                    AsyncImage(
                        model = s.photoUrl, contentDescription = s.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person, contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium)
                Text(s.career, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (s.cvUrl != null) {
                Icon(Icons.Default.Description, contentDescription = "CV analizado", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text("Aún no hay estudiantes registrados", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Toca + para agregar el primero", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAdd) { Text("Agregar estudiante") }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("No se pudo cargar la lista", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) { Text("Reintentar") }
    }
}
