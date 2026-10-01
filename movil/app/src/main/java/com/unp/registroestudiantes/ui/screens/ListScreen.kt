package com.unp.registroestudiantes.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Student
import com.unp.registroestudiantes.ui.theme.pressScale

/*
 * Archivo: ListScreen.kt
 * Proposito: Pantalla principal de exploracion con alternador dinamico entre Lista y Cuadricula.
 *
 * Guia de transicion RecyclerView (Android Clasico) y CSS Grid/Flexbox -> Jetpack Compose:
 *
 * 1. LazyColumn y LazyVerticalGrid vs RecyclerView:
 *    En Android clasico con Java, implementar una lista requeria:
 *      - Un archivo XML para el RecyclerView.
 *      - Un archivo XML para el elemento de fila (item_student.xml).
 *      - Una clase Adapter con mas de 50 lineas.
 *      - Una clase ViewHolder para cachear referencias a findViewById().
 *    En Compose, 'LazyColumn' y 'LazyVerticalGrid' hacen todo eso automaticamente
 *    reciclando los elementos fuera de la pantalla de forma transparente y eficiente.
 *
 * 2. rememberSaveable vs remember:
 *    'rememberSaveable { mutableStateOf(false) }' conserva el estado (si el usuario
 *    prefiere ver la lista o la cuadricula) incluso si rota el dispositivo o si
 *    el sistema operativo destruye temporalmente el proceso por falta de memoria.
 *
 * 3. AnimatedContent y AnimatedVisibility:
 *    - AnimatedContent: Realiza una transicion suave al alternar el icono del boton
 *      (de lista a cuadricula) con desvanecimiento cruzado (fadeIn/fadeOut).
 *    - AnimatedVisibility con retardo escalonado: Hace que las tarjetas aparezcan
 *      en cascada una tras otra (staggered animation), similar a las animaciones de listas en iOS.
 *
 * 4. AsyncImage de la libreria Coil:
 *    Descarga y decodifica la foto del estudiante desde la URL publica de Supabase
 *    de forma asincrona en segundo plano, recortandola y mostrandola sin bloquear la interfaz.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    students: List<Student>,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Student) -> Unit
) {
    // Estado del texto escrito en la barra de busqueda
    var query by remember { mutableStateOf("") }

    // Estado que define el modo de visualizacion: false = Lista, true = Cuadricula
    var isGrid by rememberSaveable { mutableStateOf(false) }

    // Filtra la lista en memoria segun el nombre o carrera ingresados
    val filtered = remember(students, query) {
        if (query.isBlank()) {
            students
        } else {
            students.filter {
                it.name.contains(query, ignoreCase = true) || it.career.contains(query, ignoreCase = true)
            }
        }
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
                    // Boton para alternar entre Vista de Lista y Vista de Cuadricula
                    IconButton(onClick = { isGrid = !isGrid }) {
                        AnimatedContent(
                            targetState = isGrid,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                            },
                            label = "viewToggle"
                        ) { grid ->
                            Icon(
                                imageVector = if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (grid) "Vista de lista" else "Vista de cuadrícula",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.pressScale()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar estudiante")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Campo de busqueda con bordes redondeados
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar por nombre o carrera") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Renderizado condicional segun el estado
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                loadError != null -> ErrorState(loadError, onRetry)
                filtered.isEmpty() && query.isBlank() -> EmptyState(onAdd)
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "Sin resultados para \"$query\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                isGrid -> {
                    // Vista en Cuadricula de 2 columnas
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filtered) { index, s ->
                            StaggeredIn(index) {
                                StudentGridCard(s) { onOpen(s) }
                            }
                        }
                    }
                }
                else -> {
                    // Vista en Lista vertical tradicional
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(filtered) { index, s ->
                            StaggeredIn(index) {
                                StudentListCard(s) { onOpen(s) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Aplica una animacion de entrada escalonada en cascada con desvanecimiento
 * y deslizamiento vertical suave.
 */
@Composable
private fun StaggeredIn(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Retarda la aparicion segun la posicion del elemento (maximo 8 escalones)
        kotlinx.coroutines.delay(minOf(index, 8) * 40L)
        shown = true
    }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 }
    ) {
        content()
    }
}

/**
 * Tarjeta de presentacion horizontal para la vista en Lista.
 */
@Composable
private fun StudentListCard(s: Student, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circular
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                if (s.photoUrl != null) {
                    AsyncImage(
                        model = s.photoUrl,
                        contentDescription = s.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Datos textuales
            Column(Modifier.weight(1f)) {
                Text(
                    text = s.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = s.career,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Icono indicador si tiene CV adjunto
            if (s.cvUrl != null) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "CV analizado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Tarjeta de presentacion vertical para la vista en Cuadricula.
 */
@Composable
private fun StudentGridCard(s: Student, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .pressScale()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Seccion superior: Foto que ocupa el espacio principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (s.photoUrl != null) {
                    AsyncImage(
                        model = s.photoUrl,
                        contentDescription = s.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Insignia circular de CV en la esquina superior derecha
                if (s.cvUrl != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "CV analizado",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Seccion inferior: Informacion del alumno
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = s.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = s.career,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Estado visual cuando no hay ningun estudiante registrado en el sistema. */
@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text("Aún no hay estudiantes registrados", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Toca + para agregar el primero",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAdd) {
            Text("Agregar estudiante")
        }
    }
}

/** Estado visual cuando ocurre un error de conexion de red. */
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Text("No se pudo cargar la lista", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}
