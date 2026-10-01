package com.unp.registroestudiantes.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.unp.registroestudiantes.data.Skill
import com.unp.registroestudiantes.data.Student
import com.unp.registroestudiantes.ui.theme.pressScale

/*
 * Archivo: DetailScreen.kt
 * Proposito: Pantalla de detalle con visor de foto completa estilo red social (WhatsApp),
 *            resena generada y aptitudes clasificadas por color.
 *
 * Guia de transicion CSS / Web Modal -> Jetpack Compose:
 *
 * 1. Visor de imagen en pantalla completa (Dialog inmersivo):
 *    'Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))'
 *    Spans 100% del ancho y alto de la pantalla fisica, sobreponiendose a la barra
 *    de navegacion del sistema (decorFitsSystemWindows = false).
 *    Equivale a un <div class="modal-overlay"> con 'position: fixed; inset: 0; background: rgba(0,0,0,0.95);'
 *    en desarrollo web.
 *
 * 2. statusBarsPadding():
 *    Agrega padding dinamico igual a la altura exacta de la barra de estado del telefono
 *    (donde se ubica la hora, la camara punch-hole o el notch), asegurando que los botones
 *    superiores no queden tapados.
 *
 * 3. FlowRow (Layout de envoltura flexible):
 *    Equivale a 'display: flex; flex-wrap: wrap; gap: 8px;' en CSS.
 *    Organiza los chips de aptitudes horizontalmente y salta de linea de manera fluida
 *    cuando se agota el ancho de la pantalla.
 *
 * 4. Chips con clasificacion semantica por color:
 *    - "tech": Azul primario para tecnologias de desarrollo (Java, Kotlin, SQL...).
 *    - "soft": Verde/Turquesa terciario para habilidades blandas (Liderazgo, Comunicacion...).
 *    - "experience": Purpura secundario para antecedentes laborales (Practicante, Pasantia...).
 *
 * 5. Intents implicitos de Android:
 *    - Intent.ACTION_SEND: Abre el panel de compartir del sistema (WhatsApp, Gmail, Telegram).
 *    - Intent.ACTION_SENDTO con "mailto:": Abre la aplicacion de correo predeterminada.
 *    - Intent.ACTION_VIEW: Abre el navegador web o visor de PDF para ver el CV adjunto.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    student: Student,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current

    // Estado para confirmar eliminacion
    var confirmDelete by remember { mutableStateOf(false) }

    // Estado para abrir el visor inmersivo de foto completa
    var showFullPhoto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    // Accion: Compartir resena
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${student.name} — ${student.career}\n\n${student.review}")
                        }
                        ctx.startActivity(Intent.createChooser(intent, "Compartir reseña"))
                    }) {
                        Icon(Icons.Default.Share, "Compartir")
                    }

                    // Accion: Editar estudiante
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Editar")
                    }

                    // Accion: Eliminar estudiante
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
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
        ) {
            // Cabecera superior con degradado visual y avatar interactivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                val hasPhoto = student.photoUrl != null

                // Avatar circular que sobresale de la cabecera
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .offset(y = 44.dp)
                        .pressScale()
                        .clickable(enabled = hasPhoto) { showFullPhoto = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(3.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto) {
                            AsyncImage(
                                model = student.photoUrl,
                                contentDescription = "Foto de ${student.name}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(58.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Insignia de lupa que indica que la foto es ampliable a pantalla completa
                    if (hasPhoto) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = 4.dp,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(32.dp)
                                .offset(x = (-2).dp, y = (-2).dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Ver foto completa",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(54.dp))

            // Informacion demografica central
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${student.career} · ${student.age} años",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Chip interactivo para redactar correo electronico
                if (!student.email.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    AssistChip(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${student.email}")
                            }
                            runCatching { ctx.startActivity(intent) }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = { Text(student.email) },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.pressScale()
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Tarjeta de Resena descriptiva
                ReviewCard(student.review)

                Spacer(Modifier.height(16.dp))

                // Tarjeta de Aptitudes clasificadas por categorias
                SectionCard(title = "Aptitudes detectadas") {
                    if (student.skills.isEmpty()) {
                        Text(
                            text = "No se detectaron aptitudes en el CV.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        FlowSkills(student.skills)
                    }
                }

                // Tarjeta interactiva para abrir el CV original
                if (student.cvUrl != null) {
                    Spacer(Modifier.height(16.dp))
                    ElevatedCard(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(student.cvUrl))
                            runCatching { ctx.startActivity(intent) }
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Currículum Vitae adjunto",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Toca para abrir o descargar el documento",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir documento",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Visor de foto completa estilo red social (WhatsApp)
    if (showFullPhoto && student.photoUrl != null) {
        Dialog(
            onDismissRequest = { showFullPhoto = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullPhoto = false }
            ) {
                // Barra superior de navegacion dentro del visor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clickable(enabled = false) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFullPhoto = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = student.career,
                            color = Color.White.copy(alpha = 0.70f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Imagen centrada a escala ajustada
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = student.photoUrl,
                        contentDescription = "Foto completa de ${student.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // Dialogo de confirmacion de borrado
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar a ${student.name}?") },
            text = { Text("Esta acción eliminará el registro y sus archivos adjuntos permanentemente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Tarjeta de resena con barra lateral de acento degradado y texto en cursiva.
 */
@Composable
private fun ReviewCard(review: String) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral con degradado
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Reseña",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = review.ifBlank { "Sin reseña disponible." },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Contenedor generico para secciones con titulo.
 */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Muestra las aptitudes como chips organizados en envoltura flexible (FlowRow)
 * con un punto de color representativo de su categoria.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowSkills(skills: List<Skill>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEachIndexed { i, skill ->
            var shown by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(i * 50L)
                shown = true
            }

            AnimatedVisibility(
                visible = shown,
                enter = scaleIn(tween(220)) + fadeIn(tween(220)),
                exit = scaleOut() + fadeOut()
            ) {
                // Color segun la categoria de la aptitud
                val dotColor = when (skill.category) {
                    "tech" -> MaterialTheme.colorScheme.primary
                    "soft" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }

                AssistChip(
                    onClick = {},
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    },
                    label = { Text(skill.name, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
