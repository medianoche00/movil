package com.unp.registroestudiantes.biometria.ui.components

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unp.registroestudiantes.biometria.util.FaceAnalyzerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Archivo: LiveFaceScannerModal.kt
 * Proposito: Escaner facial en vivo integrado dentro de la aplicacion con CameraX y ML Kit.
 *            Guia al usuario hasta posicionar su rostro, cambia el recuadro a verde al
 *            completar la captura y mantiene los errores dentro de la misma pantalla con
 *            botones para Cancelar y Reintentar.
 */

sealed class ResultadoEscaneoEnVivo {
    data class Exito(val mensaje: String) : ResultadoEscaneoEnVivo()
    data class Error(val mensaje: String) : ResultadoEscaneoEnVivo()
}

private enum class FaseEscanerFacial {
    BUSCANDO_ROSTRO,
    ALINEANDO_PROGRESO,
    VERIFICADO_VERDE,
    FALLO_ROJO
}

private const val FRAMES_ESTABLES_REQUERIDOS = 6

@Composable
fun LiveFaceScannerModal(
    visible: Boolean,
    esModoRegistro: Boolean,
    subtituloTitular: String = "",
    onCompletarEscaneo: suspend (vectorPromedio: String, frameBitmap: Bitmap) -> ResultadoEscaneoEnVivo,
    onFinalizarConExito: () -> Unit,
    onCancelar: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var fase by remember(visible) { mutableStateOf(FaseEscanerFacial.BUSCANDO_ROSTRO) }
    var mensajeGuia by remember(visible) {
        mutableStateOf("Ubica tu rostro en el centro del recuadro...")
    }
    var framesAcumulados by remember(visible) { mutableIntStateOf(0) }
    var cicloEscaneo by remember(visible) { mutableIntStateOf(0) }

    // Vinculacion de CameraX al ciclo de vida cuando se abre el modal
    DisposableEffect(visible, lifecycleOwner, previewViewRef) {
        val view = previewViewRef
        if (!visible || view == null) {
            return@DisposableEffect onDispose { }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            val provider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@Runnable
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview
                )
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    // Bucle de analisis en vivo de frames de la camara frontal
    LaunchedEffect(visible, cicloEscaneo, previewViewRef) {
        val view = previewViewRef ?: return@LaunchedEffect
        if (!visible) return@LaunchedEffect

        fase = FaseEscanerFacial.BUSCANDO_ROSTRO
        mensajeGuia = "Ubica tu rostro en el centro del recuadro..."
        framesAcumulados = 0

        val vectoresEstables = mutableListOf<String>()
        var ultimoBitmapValido: Bitmap? = null
        var ticksSinCompletar = 0

        // Pequeña pausa inicial para que la camara estabilice exposicion
        delay(450)

        while (
            fase == FaseEscanerFacial.BUSCANDO_ROSTRO ||
            fase == FaseEscanerFacial.ALINEANDO_PROGRESO
        ) {
            val frameBmp: Bitmap? = withContext(Dispatchers.Main) { view.bitmap }

            if (frameBmp != null) {
                val resultadoFrame = withContext(Dispatchers.Default) {
                    FaceAnalyzerUtil.analizarFrameEnVivo(frameBmp)
                }

                when (resultadoFrame) {
                    is FaceAnalyzerUtil.EstadoFrameFacial.RostroCentrado -> {
                        vectoresEstables.add(resultadoFrame.vectorFacial)
                        ultimoBitmapValido = resultadoFrame.frameBitmap
                        framesAcumulados = vectoresEstables.size
                        fase = FaseEscanerFacial.ALINEANDO_PROGRESO
                        mensajeGuia = "Rostro centrado · Mantén la posición..."
                        ticksSinCompletar = 0

                        if (vectoresEstables.size >= FRAMES_ESTABLES_REQUERIDOS) {
                            val vectorMaestro = withContext(Dispatchers.Default) {
                                FaceAnalyzerUtil.promediarVectores(vectoresEstables)
                            }
                            val bmpFinal = ultimoBitmapValido ?: frameBmp
                            val veredicto = onCompletarEscaneo(vectorMaestro, bmpFinal)

                            when (veredicto) {
                                is ResultadoEscaneoEnVivo.Exito -> {
                                    fase = FaseEscanerFacial.VERIFICADO_VERDE
                                    mensajeGuia = veredicto.mensaje
                                    delay(1150)
                                    onFinalizarConExito()
                                    return@LaunchedEffect
                                }

                                is ResultadoEscaneoEnVivo.Error -> {
                                    fase = FaseEscanerFacial.FALLO_ROJO
                                    mensajeGuia = veredicto.mensaje
                                    return@LaunchedEffect
                                }
                            }
                        }
                    }

                    is FaceAnalyzerUtil.EstadoFrameFacial.Desalineado -> {
                        if (vectoresEstables.isNotEmpty()) {
                            vectoresEstables.removeAt(vectoresEstables.lastIndex)
                        }
                        framesAcumulados = vectoresEstables.size
                        fase = if (framesAcumulados > 0) {
                            FaseEscanerFacial.ALINEANDO_PROGRESO
                        } else {
                            FaseEscanerFacial.BUSCANDO_ROSTRO
                        }
                        mensajeGuia = resultadoFrame.mensaje
                        ticksSinCompletar++
                    }

                    is FaceAnalyzerUtil.EstadoFrameFacial.SinRostro -> {
                        vectoresEstables.clear()
                        framesAcumulados = 0
                        fase = FaseEscanerFacial.BUSCANDO_ROSTRO
                        mensajeGuia = resultadoFrame.mensaje
                        ticksSinCompletar++
                    }
                }
            }

            if (ticksSinCompletar > 55) {
                fase = FaseEscanerFacial.FALLO_ROJO
                mensajeGuia = "No se logró estabilizar el rostro dentro del recuadro. Verifica la iluminación e intenta nuevamente."
                return@LaunchedEffect
            }

            delay(280)
        }
    }

    val colorMarco by animateColorAsState(
        targetValue = when (fase) {
            FaseEscanerFacial.BUSCANDO_ROSTRO -> Color(0xFFF59E0B)
            FaseEscanerFacial.ALINEANDO_PROGRESO -> Color(0xFF38BDF8)
            FaseEscanerFacial.VERIFICADO_VERDE -> Color(0xFF10B981)
            FaseEscanerFacial.FALLO_ROJO -> Color(0xFFEF4444)
        },
        animationSpec = tween(durationMillis = 250),
        label = "colorMarcoFacial"
    )

    val progresoAnimado by animateFloatAsState(
        targetValue = when (fase) {
            FaseEscanerFacial.VERIFICADO_VERDE -> 1f
            FaseEscanerFacial.FALLO_ROJO -> 0f
            else -> (framesAcumulados.toFloat() / FRAMES_ESTABLES_REQUERIDOS.toFloat()).coerceIn(0f, 1f)
        },
        animationSpec = tween(durationMillis = 240),
        label = "progresoEscaneo"
    )

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF060B16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Encabezado del escaner en vivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (esModoRegistro) "Registro de Rostro" else "Reconocimiento Facial",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = when {
                                esModoRegistro -> "Posiciona tu rostro hasta que el marco sea verde"
                                subtituloTitular.isNotBlank() -> subtituloTitular
                                else -> "Comparando con el titular del DNI"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onCancelar,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar escáner facial",
                            tint = Color.White
                        )
                    }
                }

                // 2. Visor central de camara en vivo con marco dinamico (Ambar -> Celeste -> Verde / Rojo)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(248.dp)
                            .height(312.dp)
                            .clip(RoundedCornerShape(46.dp))
                            .background(Color(0xFF0F172A))
                            .border(
                                width = 4.dp,
                                color = colorMarco,
                                shape = RoundedCornerShape(46.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    previewViewRef = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Marco guia interior armonizado con las esquinas redondeadas del visor
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val guideW = size.width * 0.74f
                            val guideH = size.height * 0.76f
                            drawRoundRect(
                                color = colorMarco.copy(alpha = 0.40f),
                                topLeft = Offset((size.width - guideW) / 2f, (size.height - guideH) / 2f),
                                size = Size(guideW, guideH),
                                cornerRadius = CornerRadius(34.dp.toPx(), 34.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Superposicion verde de confirmacion cuando el rostro fue validado
                        if (fase == FaseEscanerFacial.VERIFICADO_VERDE) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF10B981).copy(alpha = 0.24f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Rostro verificado",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                        }

                        // Superposicion roja sutil cuando falla la verificacion
                        if (fase == FaseEscanerFacial.FALLO_ROJO) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEF4444).copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Rostro no coincide",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Barra de progreso de estabilidad facial
                    LinearProgressIndicator(
                        progress = { progresoAnimado },
                        color = colorMarco,
                        trackColor = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .width(220.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    // Pildora o recuadro de estado dentro del escaner con altura estable
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorMarco.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, colorMarco.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = mensajeGuia,
                                color = colorMarco,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 3. Botones inferiores: Cancelar y Reintentar cuando ocurre un fallo, o Cancelar durante el escaneo
                if (fase == FaseEscanerFacial.FALLO_ROJO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelar,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Cancelar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { cicloEscaneo++ },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Reintentar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onCancelar,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancelar",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
