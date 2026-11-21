package com.unp.emprendepiura.features.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.domain.model.Conversacion
import com.unp.emprendepiura.domain.model.Mensaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversacion: Conversacion,
    mensajes: List<Mensaje>,
    miUsuarioId: String,
    loading: Boolean,
    enviando: Boolean,
    onEnviar: (String) -> Unit,
    onVolver: () -> Unit
) {
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversacion.nombreContraparte.ifBlank { "Chat" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "En tiempo real · EmprendePiura",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        placeholder = { Text("Escribe un mensaje...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        onClick = {
                            if (texto.isNotBlank()) {
                                onEnviar(texto)
                                texto = ""
                            }
                        },
                        enabled = texto.isNotBlank() && !enviando
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    ) { padding ->
        if (loading && mensajes.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(mensajes, key = { it.id.ifBlank { it.creadoEn + it.contenido } }) { msg ->
                    val esMio = msg.remitenteId == miUsuarioId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (esMio) 18.dp else 4.dp,
                                bottomEnd = if (esMio) 4.dp else 18.dp
                            ),
                            color = if (esMio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                                Text(
                                    text = msg.contenido,
                                    fontSize = 14.sp,
                                    color = if (esMio) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                val hora = msg.creadoEn.drop(11).take(5)
                                if (hora.isNotBlank()) {
                                    Text(
                                        text = hora,
                                        fontSize = 10.sp,
                                        color = if (esMio) Color.White.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
