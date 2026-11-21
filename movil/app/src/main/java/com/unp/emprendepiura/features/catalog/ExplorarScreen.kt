package com.unp.emprendepiura.features.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unp.emprendepiura.core.theme.AppleNavigationEasing
import com.unp.emprendepiura.core.theme.applePushSpring
import com.unp.emprendepiura.core.theme.pressScale
import com.unp.emprendepiura.core.theme.rememberPressInteraction
import com.unp.emprendepiura.core.ui.EmprendePiuraBrandBadge
import com.unp.emprendepiura.core.ui.EmptyStateBox
import com.unp.emprendepiura.core.ui.ProductThumbnail
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.domain.model.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorarScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onSelectCategoria: (String?) -> Unit,
    onSelectZona: (String?) -> Unit,
    onRefresh: () -> Unit,
    onAbrirProducto: (Producto) -> Unit,
    modifier: Modifier = Modifier
) {
    val productos = state.productosFiltrados

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EmprendePiuraBrandBadge(
                        size = 38.dp,
                        cornerRadius = 11.dp,
                        elevatedShadow = false
                    )
                    Column {
                        Text("EmprendePiura", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                        Text(
                            text = "Emprendedores locales · Piura y UNP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar catálogo")
                }
            }
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar comida, apuntes, artesanía...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Filtros por categoria
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.categoriaSeleccionadaId == null,
                    onClick = { onSelectCategoria(null) },
                    label = { Text("Todo", fontSize = 12.5.sp) },
                    shape = RoundedCornerShape(50)
                )
            }
            items(state.categorias, key = { it.id }) { cat ->
                FilterChip(
                    selected = state.categoriaSeleccionadaId == cat.id,
                    onClick = {
                        onSelectCategoria(if (state.categoriaSeleccionadaId == cat.id) null else cat.id)
                    },
                    label = { Text(cat.nombre, fontSize = 12.5.sp) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        // Filtros por zona local de Piura / UNP
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AssistChip(
                    onClick = { onSelectZona(null) },
                    label = {
                        Text(
                            text = if (state.zonaSeleccionada == null) "Todas las zonas" else "Limpiar zona",
                            fontSize = 11.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    shape = RoundedCornerShape(50)
                )
            }
            items(PiuraConstants.ZONAS_PIURA) { zona ->
                FilterChip(
                    selected = state.zonaSeleccionada == zona,
                    onClick = { onSelectZona(if (state.zonaSeleccionada == zona) null else zona) },
                    label = { Text(zona, fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(50)
                )
            }
        }

        val estadoVista = when {
            state.loading && state.productos.isEmpty() -> 0
            state.error != null && state.productos.isEmpty() -> 1
            productos.isEmpty() -> 2
            else -> 3
        }

        AnimatedContent(
            targetState = estadoVista,
            transitionSpec = {
                fadeIn(tween(240, easing = AppleNavigationEasing)) togetherWith
                    fadeOut(tween(170))
            },
            modifier = Modifier.weight(1f),
            label = "explorarCatalogState"
        ) { modo ->
            when (modo) {
                0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                1 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateBox(
                            icon = Icons.Default.Storefront,
                            titulo = "Sin conexión al catálogo",
                            subtitulo = state.error ?: "No pudimos cargar el catálogo.",
                            accionTexto = "Reintentar",
                            onAccionClick = onRefresh
                        )
                    }
                }

                2 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyStateBox(
                            icon = Icons.Default.Search,
                            titulo = "No hay productos coincidentes",
                            subtitulo = "Prueba limpiando los filtros o buscando otro término."
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(applePushSpring())
                    ) {
                        items(productos, key = { it.id }) { prod ->
                            ProductoGridCard(
                                producto = prod,
                                onClick = { onAbrirProducto(prod) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(260),
                                    placementSpec = applePushSpring()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoGridCard(
    producto: Producto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val press = rememberPressInteraction()
    Surface(
        onClick = onClick,
        interactionSource = press,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(press)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ProductThumbnail(
                    imageUrl = producto.imagenUrl,
                    categoria = producto.nombreCategoria,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = producto.zonaPiura,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = PiuraConstants.formatearSoles(producto.precio),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = producto.titulo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = producto.nombreTienda,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (producto.stock > 0) "Stock ${producto.stock}" else "Agotado",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (producto.stock > 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
