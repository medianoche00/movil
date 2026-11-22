package com.unp.emprendepiura.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.EstadoOrden
import com.unp.emprendepiura.domain.model.Orden
import com.unp.emprendepiura.domain.model.RolUsuario
import com.unp.emprendepiura.domain.model.Usuario

/*
 * Archivo: AdminDetailTabs.kt
 * Proposito: Sub-pantallas modulares del Panel Administrativo para gestion de usuarios
 *            registrados (DNIs y habilitar/deshabilitar) y auditoria de pedidos/ventas.
 */
@Composable
fun AdminUsersTab(
    usuarios: List<Usuario>,
    miAdminUsuarioId: String,
    onToggleUsuarioActivo: (Usuario) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableIntStateOf(0) } // 0: Todos, 1: Activos, 2: Deshabilitados
    var usuarioConfirmar by remember { mutableStateOf<Usuario?>(null) }

    val filtrados = remember(usuarios, query, filtroEstado) {
        val q = query.trim().lowercase()
        usuarios.filter { u ->
            val okEstado = when (filtroEstado) {
                1 -> u.activo
                2 -> !u.activo
                else -> true
            }
            val okQuery = q.isEmpty() ||
                u.nombre.lowercase().contains(q) ||
                u.dni.contains(q) ||
                u.correo.lowercase().contains(q) ||
                u.rol.etiqueta.lowercase().contains(q)
            okEstado && okQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar por nombre, DNI o rol...", fontSize = 13.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(19.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filtroEstado == 0,
                onClick = { filtroEstado = 0 },
                label = { Text("Todos (${usuarios.size})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = filtroEstado == 1,
                onClick = { filtroEstado = 1 },
                label = { Text("Activos (${usuarios.count { it.activo }})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = filtroEstado == 2,
                onClick = { filtroEstado = 2 },
                label = { Text("Deshabilitados (${usuarios.count { !it.activo }})", fontSize = 12.sp) }
            )
        }

        if (filtrados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.Default.Person,
                    titulo = "Sin usuarios coincidentes",
                    subtitulo = "Ajusta la búsqueda o el filtro para ver las cuentas registradas."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtrados, key = { it.id.ifBlank { it.dni } }) { user ->
                    val esMiCuenta = user.id == miAdminUsuarioId
                    val colorRol = when (user.rol) {
                        RolUsuario.ADMIN -> Color(0xFF6366F1)
                        RolUsuario.EMPRENDEDOR -> Color(0xFF15803D)
                        RolUsuario.COMPRADOR -> Color(0xFF2563EB)
                    }

                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            if (user.activo) colorRol.copy(alpha = 0.14f)
                                            else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.iniciales,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (user.activo) colorRol else MaterialTheme.colorScheme.error
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (esMiCuenta) "${user.nombre} (Tú)" else user.nombre,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "DNI: ${user.dni} · ${user.zonaPiura}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                StatusPillBadge(
                                    texto = if (user.activo) "Activo" else "Deshabilitado",
                                    color = if (user.activo) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPillBadge(texto = user.rol.etiqueta, color = colorRol)
                                if (user.huella.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Huella registrada",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (user.vectorFacial.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Rostro registrado",
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                if (user.telefono.isNotBlank()) {
                                    Text(
                                        text = "Tel: ${user.telefono}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (user.correo.isNotBlank()) {
                                Text(
                                    text = user.correo,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!esMiCuenta) {
                                if (user.activo) {
                                    OutlinedButton(
                                        onClick = { usuarioConfirmar = user },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Deshabilitar usuario", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = { usuarioConfirmar = user },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Habilitar usuario", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    usuarioConfirmar?.let { target ->
        val vaADeshabilitar = target.activo
        AlertDialog(
            onDismissRequest = { usuarioConfirmar = null },
            icon = {
                Icon(
                    imageVector = if (vaADeshabilitar) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (vaADeshabilitar) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                )
            },
            title = {
                Text(
                    text = if (vaADeshabilitar) "¿Deshabilitar usuario?" else "¿Habilitar usuario?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (vaADeshabilitar) {
                        "¿Estás seguro de deshabilitar a ${target.nombre} (DNI ${target.dni})? No podrá iniciar sesión hasta que lo vuelvas a habilitar."
                    } else {
                        "¿Estás seguro de habilitar nuevamente la cuenta de ${target.nombre} (DNI ${target.dni})?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        usuarioConfirmar = null
                        onToggleUsuarioActivo(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vaADeshabilitar) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                    )
                ) {
                    Text(if (vaADeshabilitar) "Sí, deshabilitar" else "Sí, habilitar")
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioConfirmar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AdminOrdersTab(
    ordenes: List<Orden>,
    montoTotalVentas: Double
) {
    var estadoFiltro by remember { mutableStateOf<EstadoOrden?>(null) }

    val filtradas = remember(ordenes, estadoFiltro) {
        if (estadoFiltro == null) ordenes else ordenes.filter { it.estado == estadoFiltro }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF15803D).copy(alpha = 0.09f),
            border = BorderStroke(1.dp, Color(0xFF15803D).copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Volumen de ventas activas", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = PiuraConstants.formatearSoles(montoTotalVentas),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF15803D)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${ordenes.size} pedidos totales", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Text(
                        text = "${ordenes.count { it.estado == EstadoOrden.ENTREGADO }} entregados",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = estadoFiltro == null,
                onClick = { estadoFiltro = null },
                label = { Text("Todos (${ordenes.size})", fontSize = 12.sp) }
            )
            EstadoOrden.entries.forEach { est ->
                val count = ordenes.count { it.estado == est }
                FilterChip(
                    selected = estadoFiltro == est,
                    onClick = { estadoFiltro = if (estadoFiltro == est) null else est },
                    label = { Text("${est.etiqueta} ($count)", fontSize = 12.sp) }
                )
            }
        }

        if (filtradas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateBox(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    titulo = "Sin pedidos en esta categoría",
                    subtitulo = "Los pedidos generados por compradores aparecerán aquí en tiempo real."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtradas, key = { it.id }) { orden ->
                    val colorEst = when (orden.estado) {
                        EstadoOrden.PENDIENTE -> Color(0xFFD97706)
                        EstadoOrden.CONFIRMADO -> Color(0xFF2563EB)
                        EstadoOrden.EN_CAMINO -> Color(0xFF7C3AED)
                        EstadoOrden.ENTREGADO -> Color(0xFF15803D)
                        EstadoOrden.CANCELADO -> MaterialTheme.colorScheme.error
                    }
                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(orden.codigoOrden, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                                StatusPillBadge(orden.estado.etiqueta, colorEst)
                            }
                            Text(
                                text = "Tienda: ${orden.nombreTienda} · Comprador: ${orden.nombreComprador}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${orden.tipoEntrega.etiqueta} · ${orden.metodoPago.etiqueta}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = PiuraConstants.formatearSoles(orden.total),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
