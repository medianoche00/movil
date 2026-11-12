package com.unp.registroestudiantes.biometria.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Archivo: WelcomeAuthScreen.kt
 * Proposito: Pantalla principal de bienvenida (Rediseño v4 · Modo A) con ingreso de DNI primero,
 *            despliegue inline de contrasena y 3 metodos de acceso: Huella, Rostro y Contrasena.
 */
@Composable
fun WelcomeAuthScreen(
    dni: String,
    contrasena: String,
    mostrarCampoContrasena: Boolean,
    dniInlineError: String?,
    dniShakeTrigger: Int,
    loading: Boolean,
    darkTheme: Boolean,
    onDniChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onIngresarConHuella: () -> Unit,
    onIngresarConRostro: () -> Unit,
    onMostrarContrasenaClick: () -> Unit,
    onOcultarContrasenaClick: () -> Unit,
    onConfirmarContrasena: () -> Unit,
    onIrARegistro: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var verContrasena by remember { mutableStateOf(false) }
    val passFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shakeOffset = remember { Animatable(0f) }

    // Sacudida horizontal suave del campo DNI si se intenta acceder sin los 8 digitos
    LaunchedEffect(dniShakeTrigger) {
        if (dniShakeTrigger > 0) {
            val steps = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            for (target in steps) {
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 42))
            }
        }
    }

    // Enfocar automaticamente el campo de contrasena cuando se despliega
    LaunchedEffect(mostrarCampoContrasena) {
        if (mostrarCampoContrasena) {
            delay(120)
            runCatching { passFocusRequester.requestFocus() }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Barra superior minimalista: unicamente el boton de Tema a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tema",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2. Bloque central: Emblema + ¡Bienvenido! + DNI Primero + Contrasena Inline + 3 Botones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(19.dp),
                            spotColor = Color(0xFF2563EB).copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "¡Bienvenido!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Ingresa tu DNI para iniciar sesión",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Campo de DNI destacado en la pantalla principal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DNI Usuario *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dni.length}/8",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = dni,
                        onValueChange = onDniChange,
                        placeholder = { Text("8 dígitos numéricos", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        isError = dniInlineError != null,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (mostrarCampoContrasena) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dniInlineError != null) {
                        Text(
                            text = dniInlineError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                        )
                    }
                }

                // Despliegue Inline de Contrasena (Modo A)
                AnimatedVisibility(
                    visible = mostrarCampoContrasena,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkTheme) 0.35f else 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Contraseña *",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ocultar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable(enabled = !loading) {
                                            focusManager.clearFocus()
                                            onOcultarContrasenaClick()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            OutlinedTextField(
                                value = contrasena,
                                onValueChange = onContrasenaChange,
                                placeholder = { Text("Ingresa tu contraseña", fontSize = 14.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { verContrasena = !verContrasena }) {
                                        Icon(
                                            imageVector = if (verContrasena) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = "Mostrar u ocultar contraseña",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (verContrasena) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        onConfirmarContrasena()
                                    }
                                ),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(passFocusRequester)
                            )

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    onConfirmarContrasena()
                                },
                                enabled = !loading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(17.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verificando...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Text("Ingresar a mi cuenta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Divisor sutil "ELIGE TU METODO DE ACCESO"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "ELIGE TU MÉTODO DE ACCESO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Opcion 1: Ingresa con Huella
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onIngresarConHuella()
                    },
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Ingresa con Huella",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Opcion 2: Ingresa con Rostro
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        onIngresarConRostro()
                    },
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF2563EB).copy(alpha = if (darkTheme) 0.16f else 0.09f),
                        contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    ),
                    border = BorderStroke(
                        width = 1.4.dp,
                        color = Color(0xFF2563EB).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Ingresa con Rostro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Opcion 3: Ingresa con tu Contrasena
                Surface(
                    onClick = onMostrarContrasenaClick,
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Ingresa con tu Contraseña",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            // 3. Pie de pantalla con version y enlace Registrate
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "v1.0",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(10.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¿No tienes cuenta? ",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Regístrate",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable(enabled = !loading) { onIrARegistro() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
