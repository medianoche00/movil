package com.unp.emprendepiura.features.marketplace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.EmprendePiuraBrandBadge
import com.unp.emprendepiura.core.ui.StatusPillBadge
import com.unp.emprendepiura.domain.model.Emprendimiento
import com.unp.emprendepiura.domain.model.EstadoEmprendimiento
import com.unp.emprendepiura.domain.model.RolUsuario
import com.unp.emprendepiura.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilMarketplaceScreen(
    usuario: Usuario?,
    miTienda: Emprendimiento?,
    entregasPendientesCount: Int,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAbrirPedidosPorEntregar: () -> Unit,
    onAbrirMiTienda: () -> Unit,
    onSolicitarTienda: () -> Unit,
    onAbrirPanelAdmin: () -> Unit,
    onEditarDatosPerfil: () -> Unit,
    onRecapturarHuella: () -> Unit,
    onRecapturarRostro: () -> Unit,
    onIrALogin: () -> Unit,
    onIrARegistro: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mi Perfil", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
            actions = {
                val temaPress = rememberPressInteraction()
                TextButton(
                    onClick = onToggleTheme,
                    interactionSource = temaPress,
                    modifier = Modifier.pressScale(temaPress)
                ) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Tema", fontWeight = FontWeight.SemiBold)
                }
            }
        )

        AnimatedContent(
            targetState = usuario,
            transitionSpec = {
                (fadeIn(tween(260, easing = AppleNavigationEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = applePushSpring())) togetherWith
                    fadeOut(tween(170))
            },
            label = "perfilAuthContent"
        ) { currentUser ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .animateContentSize(applePushSpring()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (currentUser == null) {
                    InvitadoPerfilContent(
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        onIrALogin = onIrALogin,
                        onIrARegistro = onIrARegistro
                    )
                } else {
                    UsuarioAutenticadoContent(
                        usuario = currentUser,
                        miTienda = miTienda,
                        entregasPendientesCount = entregasPendientesCount,
                        onAbrirPedidosPorEntregar = onAbrirPedidosPorEntregar,
                        onAbrirMiTienda = onAbrirMiTienda,
                        onSolicitarTienda = onSolicitarTienda,
                        onAbrirPanelAdmin = onAbrirPanelAdmin,
                        onEditarDatosPerfil = onEditarDatosPerfil,
                        onRecapturarHuella = onRecapturarHuella,
                        onRecapturarRostro = onRecapturarRostro,
                        onCerrarSesion = onCerrarSesion
                    )
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun InvitadoPerfilContent(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onIrALogin: () -> Unit,
    onIrARegistro: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera hero con degradado sutil de marca
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = if (darkTheme) 0.22f else 0.10f),
                                Color(0xFFF59E0B).copy(alpha = if (darkTheme) 0.06f else 0.04f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmprendePiuraBrandBadge(
                        size = 64.dp,
                        cornerRadius = 20.dp,
                        elevatedShadow = true
                    )

                    Spacer(Modifier.height(2.dp))

                    StatusPillBadge(
                        texto = "MODO INVITADO",
                        color = Color(0xFF2563EB)
                    )

                    Text(
                        text = "Tu espacio en EmprendePiura",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Conecta con emprendedores de Piura y Campus UNP para comprar, chatear en vivo o abrir tu propia tienda.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.35f else 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GuestBenefitRow(
                            icon = Icons.Default.LocalMall,
                            tint = Color(0xFF2563EB),
                            titulo = "Pedidos locales y seguimiento",
                            subtitulo = "Compra con entrega en campus UNP o puntos de Piura"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        GuestBenefitRow(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            tint = Color(0xFF0D9488),
                            titulo = "Chat directo con tiendas",
                            subtitulo = "Coordina pagos Yape/Plin y recojo en tiempo real"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        GuestBenefitRow(
                            icon = Icons.Default.Fingerprint,
                            tint = Color(0xFFD97706),
                            titulo = "Acceso rápido y biométrico",
                            subtitulo = "Protege tu cuenta con huella dactilar y rostro facial"
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                val loginPress = rememberPressInteraction()
                Button(
                    onClick = onIrALogin,
                    interactionSource = loginPress,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(loginPress)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }

                val regPress = rememberPressInteraction()
                OutlinedButton(
                    onClick = onIrARegistro,
                    interactionSource = regPress,
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF2563EB).copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF2563EB).copy(alpha = if (darkTheme) 0.12f else 0.06f),
                        contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(regPress)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Cuenta Gratis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    Text(
        text = "PREFERENCIAS E INFORMACIÓN",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )

    val themeCardPress = rememberPressInteraction()
    Surface(
        onClick = onToggleTheme,
        interactionSource = themeCardPress,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(themeCardPress)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Apariencia de la app", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = if (darkTheme) "Modo oscuro activado" else "Modo claro activado",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = darkTheme,
                onCheckedChange = { onToggleTheme() }
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D9488).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF0D9488),
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Cobertura Marketplace", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Campus UNP · Piura Centro · Castilla · Veintiséis de Octubre · Catacaos",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Text(
        text = "EmprendePiura · Comercio Local UNP · v1.0",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

@Composable
private fun GuestBenefitRow(
    icon: ImageVector,
    tint: Color,
    titulo: String,
    subtitulo: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitulo, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UsuarioAutenticadoContent(
    usuario: Usuario,
    miTienda: Emprendimiento?,
    entregasPendientesCount: Int,
    onAbrirPedidosPorEntregar: () -> Unit,
    onAbrirMiTienda: () -> Unit,
    onSolicitarTienda: () -> Unit,
    onAbrirPanelAdmin: () -> Unit,
    onEditarDatosPerfil: () -> Unit,
    onRecapturarHuella: () -> Unit,
    onRecapturarRostro: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val foto = usuario.fotoPerfil.takeIf { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (foto != null) {
                    AsyncImage(model = foto, contentDescription = usuario.nombre, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(usuario.iniciales, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(usuario.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("DNI: ${usuario.dni} · ${usuario.zonaPiura}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatusPillBadge(texto = usuario.rol.etiqueta, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    Text(
        text = "EMPRENDIMIENTO Y GESTIÓN",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    when (miTienda?.estado) {
        null -> {
            PerfilActionCard(
                icon = Icons.Default.Storefront,
                tint = Color(0xFF0D9488),
                titulo = "Quiero vender en EmprendePiura",
                subtitulo = "Registra tu emprendimiento local para publicar productos",
                onClick = onSolicitarTienda
            )
        }
        EstadoEmprendimiento.PENDIENTE -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFD97706).copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Solicitud en revisión: ${miTienda.nombreTienda}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Un administrador revisará y aprobará tu tienda pronto.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        EstadoEmprendimiento.APROBADO -> {
            PerfilActionCard(
                icon = Icons.Default.LocalShipping,
                tint = MaterialTheme.colorScheme.primary,
                titulo = if (entregasPendientesCount > 0) {
                    "Pedidos Por Entregar ($entregasPendientesCount)"
                } else {
                    "Pedidos Por Entregar"
                },
                subtitulo = "Gestiona entregas y valida PIN dinámico desde Pedidos",
                onClick = onAbrirPedidosPorEntregar
            )
            PerfilActionCard(
                icon = Icons.Default.Storefront,
                tint = Color(0xFF15803D),
                titulo = "Catálogo de Mi Tienda (${miTienda.nombreTienda})",
                subtitulo = "Publicar, editar o pausar productos y controlar stock",
                onClick = onAbrirMiTienda
            )
        }
        EstadoEmprendimiento.SUSPENDIDO -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Emprendimiento suspendido: ${miTienda.nombreTienda}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }

    if (usuario.rol == RolUsuario.ADMIN) {
        PerfilActionCard(
            icon = Icons.Default.AdminPanelSettings,
            tint = Color(0xFF6366F1),
            titulo = "Panel Administrativo",
            subtitulo = "Aprobar emprendimientos, categorías, moderación y métricas",
            onClick = onAbrirPanelAdmin
        )
    }

    Text(
        text = "CUENTA Y SEGURIDAD BIOMÉTRICA",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    PerfilActionCard(
        icon = Icons.Default.Edit,
        tint = MaterialTheme.colorScheme.primary,
        titulo = "Editar datos de cuenta",
        subtitulo = "Actualizar nombre, celular, zona o contraseña",
        onClick = onEditarDatosPerfil
    )

    PerfilActionCard(
        icon = Icons.Default.Fingerprint,
        tint = Color(0xFF15803D),
        titulo = if (usuario.huella.isBlank()) "Vincular Huella Dactilar" else "Cambiar Huella Dactilar",
        subtitulo = "Configura tu huella en el lector físico del dispositivo",
        onClick = onRecapturarHuella
    )

    PerfilActionCard(
        icon = Icons.Default.Face,
        tint = Color(0xFF0D9488),
        titulo = if (usuario.vectorFacial.isBlank()) "Registrar Rostro Facial" else "Recapturar Rostro Facial",
        subtitulo = "Actualiza tu reconocimiento facial con MobileFaceNet",
        onClick = onRecapturarRostro
    )

    Spacer(Modifier.height(6.dp))

    val logoutPress = rememberPressInteraction()
    OutlinedButton(
        onClick = onCerrarSesion,
        interactionSource = logoutPress,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(logoutPress)
    ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun PerfilActionCard(
    icon: ImageVector,
    tint: Color,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    val press = rememberPressInteraction()
    Surface(
        onClick = onClick,
        interactionSource = press,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(press)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitulo, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
