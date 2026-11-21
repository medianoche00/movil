package com.unp.emprendepiura.features.auth.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.emprendepiura.core.util.HashUtil
import com.unp.emprendepiura.core.util.PiuraConstants
import com.unp.emprendepiura.data.dto.UsuarioUpsertDto
import com.unp.emprendepiura.data.repository.AuthBiometricRepository
import com.unp.emprendepiura.domain.model.Usuario
import com.unp.emprendepiura.domain.validator.BiometricValidator
import com.unp.emprendepiura.features.auth.biometric.FaceAnalyzerUtil
import com.unp.emprendepiura.features.auth.biometric.FaceComparisonEngine
import com.unp.emprendepiura.features.auth.ui.components.ResultadoEscaneoEnVivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Archivo: BiometricViewModel.kt
 * Proposito: Coordina login biometrico 1:1 (Huella, Rostro MobileFaceNet y Contrasena SHA-256),
 *            registro unificado de usuarios en Supabase (public.usuarios) y recaptura en sesion.
 */
class BiometricViewModel(
    private val repository: AuthBiometricRepository = AuthBiometricRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricUiState())
    val state: StateFlow<BiometricUiState> = _state.asStateFlow()

    private var dniInicializado = false
    private var usuarioObjetivoFacial: Usuario? = null
    private var similitudFacialTemp: Int = 0

    fun inicializarDniRecordado(context: Context) {
        if (dniInicializado) return
        dniInicializado = true
        val recordado = repository.obtenerUltimoDniRecordado(context)
        if (recordado.length == 8 && _state.value.loginDni.isEmpty()) {
            _state.update { it.copy(loginDni = recordado) }
            viewModelScope.launch {
                val existe = withContext(Dispatchers.IO) {
                    repository.getByDni(context, recordado) != null
                }
                if (!existe && _state.value.loginDni == recordado) {
                    _state.update { it.copy(loginDni = "", dniInlineError = null) }
                }
            }
        }
    }

    fun onLoginDniChange(v: String) {
        val soloDigitos = v.filter { it.isDigit() }.take(8)
        _state.update { it.copy(loginDni = soloDigitos, dniInlineError = null) }
    }

    fun onLoginContrasenaChange(v: String) {
        _state.update { it.copy(loginContrasena = v) }
    }

    fun ocultarContrasenaInline() {
        _state.update { it.copy(mostrarCampoContrasena = false, loginContrasena = "") }
    }

    fun onRegDniChange(v: String, context: Context) {
        val soloDigitos = v.filter { it.isDigit() }.take(8)
        _state.update {
            it.copy(
                regDni = soloDigitos,
                esModoEdicion = if (soloDigitos.length < 8) false else it.esModoEdicion
            )
        }
        if (soloDigitos.length == 8) {
            buscarYPrecargarUsuario(context, soloDigitos)
        }
    }

    fun onRegNombreChange(v: String) = _state.update { it.copy(regNombre = v) }
    fun onRegTelefonoChange(v: String) = _state.update { it.copy(regTelefono = v.filter { c -> c.isDigit() }.take(12)) }
    fun onRegCorreoChange(v: String) = _state.update { it.copy(regCorreo = v) }
    fun onRegZonaChange(v: String) = _state.update { it.copy(regZonaPiura = v) }
    fun onRegContrasenaChange(v: String) = _state.update { it.copy(regContrasena = v) }

    fun buscarYPrecargarUsuario(context: Context, dni: String) {
        if (dni.length != 8) return
        viewModelScope.launch {
            _state.update { it.copy(buscandoUsuarioDni = true) }
            val usuario = withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, dni) }.getOrNull()
            }
            _state.update { it.copy(buscandoUsuarioDni = false) }

            if (usuario != null) {
                val fotoUri = usuario.fotoPerfil.takeIf { it.isNotBlank() }?.let {
                    runCatching { Uri.parse(it) }.getOrNull()
                }
                _state.update {
                    it.copy(
                        regDni = usuario.dni,
                        regNombre = usuario.nombre,
                        regTelefono = usuario.telefono,
                        regCorreo = usuario.correo,
                        regZonaPiura = usuario.zonaPiura.ifBlank { PiuraConstants.ZONAS_PIURA.first() },
                        regContrasena = "",
                        regPasswordHashActual = usuario.passwordHash,
                        regHuellaToken = usuario.huella,
                        regFotoFacialUri = fotoUri,
                        regFotoFacialBytes = null,
                        regVectorFacial = usuario.vectorFacial,
                        esModoEdicion = true,
                        errorMessage = null,
                        successMessage = null
                    )
                }
            } else {
                _state.update { it.copy(esModoEdicion = false, regPasswordHashActual = "") }
            }
        }
    }

    fun irARegistro(context: Context? = null) {
        val dniActual = _state.value.loginDni.trim()
        _state.update {
            it.copy(
                vistaActual = BiometricVista.REGISTRO,
                regDni = if (dniActual.length == 8) dniActual else "",
                regNombre = "",
                regTelefono = "",
                regCorreo = "",
                regZonaPiura = PiuraConstants.ZONAS_PIURA.first(),
                regContrasena = "",
                regPasswordHashActual = "",
                regHuellaToken = "",
                regFotoFacialUri = null,
                regFotoFacialBytes = null,
                regVectorFacial = "",
                esModoEdicion = false,
                mostrarCampoContrasena = false,
                dniInlineError = null,
                errorMessage = null,
                successMessage = null
            )
        }
        if (context != null && dniActual.length == 8) {
            buscarYPrecargarUsuario(context, dniActual)
        }
    }

    fun irAEditarBiometria(context: Context) {
        val user = _state.value.usuarioAutenticado
        if (user != null) {
            val fotoUri = user.fotoPerfil.takeIf { it.isNotBlank() }?.let {
                runCatching { Uri.parse(it) }.getOrNull()
            }
            _state.update {
                it.copy(
                    vistaActual = BiometricVista.REGISTRO,
                    regDni = user.dni,
                    regNombre = user.nombre,
                    regTelefono = user.telefono,
                    regCorreo = user.correo,
                    regZonaPiura = user.zonaPiura.ifBlank { PiuraConstants.ZONAS_PIURA.first() },
                    regContrasena = "",
                    regPasswordHashActual = user.passwordHash,
                    regHuellaToken = user.huella,
                    regFotoFacialUri = fotoUri,
                    regFotoFacialBytes = null,
                    regVectorFacial = user.vectorFacial,
                    esModoEdicion = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
        } else {
            irARegistro(context)
        }
    }

    fun volverABienvenida() {
        val s = _state.value
        if (s.usuarioAutenticado != null) {
            _state.update {
                it.copy(
                    vistaActual = BiometricVista.MARKETPLACE,
                    mostrarModalHuella = false,
                    mostrarEscanerFacial = false,
                    esModoEdicion = false,
                    buscandoUsuarioDni = false,
                    regDni = "",
                    regNombre = "",
                    regTelefono = "",
                    regCorreo = "",
                    regContrasena = "",
                    regPasswordHashActual = "",
                    regHuellaToken = "",
                    regFotoFacialUri = null,
                    regFotoFacialBytes = null,
                    regVectorFacial = "",
                    errorMessage = null,
                    successMessage = null
                )
            }
            return
        }

        val dni = s.regDni
        _state.update {
            it.copy(
                vistaActual = BiometricVista.BIENVENIDA,
                mostrarModalHuella = false,
                mostrarEscanerFacial = false,
                esModoEdicion = false,
                buscandoUsuarioDni = false,
                loginDni = if (dni.length == 8) dni else it.loginDni,
                regDni = "",
                regNombre = "",
                regTelefono = "",
                regCorreo = "",
                regContrasena = "",
                regPasswordHashActual = "",
                regHuellaToken = "",
                regFotoFacialUri = null,
                regFotoFacialBytes = null,
                regVectorFacial = "",
                dniInlineError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cerrarSesion() {
        _state.update {
            it.copy(
                vistaActual = BiometricVista.BIENVENIDA,
                loginContrasena = "",
                mostrarCampoContrasena = false,
                usuarioAutenticado = null,
                metodoAutenticacion = "",
                esModoEdicion = false,
                buscandoUsuarioDni = false,
                regDni = "",
                regNombre = "",
                regTelefono = "",
                regCorreo = "",
                regContrasena = "",
                regPasswordHashActual = "",
                regHuellaToken = "",
                regFotoFacialUri = null,
                regFotoFacialBytes = null,
                regVectorFacial = "",
                errorMessage = null,
                successMessage = "Sesión cerrada correctamente."
            )
        }
    }

    fun refrescarUsuarioAutenticado(context: Context) {
        val dni = _state.value.usuarioAutenticado?.dni ?: return
        viewModelScope.launch {
            val fresco = withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, dni) }.getOrNull()
            }
            if (fresco != null) {
                _state.update { it.copy(usuarioAutenticado = fresco) }
            }
        }
    }

    private fun validarDniBienvenida(): String? {
        val dni = _state.value.loginDni.trim()
        if (dni.length != 8) {
            _state.update {
                it.copy(
                    dniInlineError = "Ingresa los 8 dígitos del DNI para continuar.",
                    dniShakeTrigger = it.dniShakeTrigger + 1
                )
            }
            return null
        }
        _state.update { it.copy(dniInlineError = null) }
        return dni
    }

    fun desplegarContrasenaInline(context: Context) {
        val dni = validarDniBienvenida() ?: return
        if (_state.value.mostrarCampoContrasena) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, successMessage = null) }
            val usuario = withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, dni) }.getOrNull()
            }
            _state.update { it.copy(loading = false) }

            if (usuario == null) {
                _state.update {
                    it.copy(errorMessage = "El DNI $dni no se encuentra registrado. Usa Regístrate.")
                }
                return@launch
            }

            _state.update { it.copy(mostrarCampoContrasena = true, errorMessage = null) }
        }
    }

    fun loginConContrasena(context: Context) {
        val dni = validarDniBienvenida() ?: return
        val pass = _state.value.loginContrasena
        if (pass.isBlank()) {
            _state.update {
                it.copy(errorMessage = "Ingresa tu contraseña para continuar.", successMessage = null)
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, successMessage = null) }
            try {
                val usuario = withContext(Dispatchers.IO) { repository.getByDni(context, dni) }
                if (usuario == null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            errorMessage = "El DNI $dni no se encuentra registrado. Usa Regístrate."
                        )
                    }
                    return@launch
                }
                if (!HashUtil.verifyPassword(pass, usuario.passwordHash)) {
                    _state.update {
                        it.copy(loading = false, errorMessage = "La contraseña ingresada es incorrecta.")
                    }
                    return@launch
                }

                repository.recordarUltimoDni(context, usuario.dni)
                _state.update {
                    it.copy(
                        loading = false,
                        loginContrasena = "",
                        mostrarCampoContrasena = false,
                        usuarioAutenticado = usuario,
                        metodoAutenticacion = "Verificado por Contraseña",
                        vistaActual = BiometricVista.MARKETPLACE,
                        successMessage = "Bienvenido a EmprendePiura, ${usuario.nombre}."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, errorMessage = BiometricValidator.mapearError(e, dni))
                }
            }
        }
    }

    fun abrirModalHuellaLogin(context: Context) {
        val dni = validarDniBienvenida() ?: return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, successMessage = null) }
            val usuario = withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, dni) }.getOrNull()
            }
            _state.update { it.copy(loading = false) }

            if (usuario == null) {
                _state.update {
                    it.copy(errorMessage = "El DNI $dni no se encuentra registrado. Usa Regístrate.")
                }
                return@launch
            }

            if (usuario.huella.isBlank()) {
                _state.update {
                    it.copy(
                        mostrarModalHuella = true,
                        huellaEnModoRegistro = false,
                        huellaDniObjetivo = usuario.dni,
                        huellaSubtitulo = "${usuario.nombre} · DNI ${usuario.dni}",
                        huellaUsuarioObjetivo = null,
                        huellaErrorInicial = "Esta cuenta aún no tiene huella vinculada. Entra con tu Contraseña o vincúlala en Perfil."
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        mostrarModalHuella = true,
                        huellaEnModoRegistro = false,
                        huellaDniObjetivo = usuario.dni,
                        huellaSubtitulo = "${usuario.nombre} · DNI ${usuario.dni}",
                        huellaUsuarioObjetivo = usuario,
                        huellaErrorInicial = null
                    )
                }
            }
        }
    }

    fun abrirModalHuellaRegistro() {
        val s = _state.value
        val errorDni = BiometricValidator.validarDni(s.regDni)
        if (errorDni != null) {
            _state.update {
                it.copy(
                    errorMessage = "Primero ingresa los 8 dígitos del DNI antes de registrar tu huella.",
                    successMessage = null
                )
            }
            return
        }

        _state.update {
            it.copy(
                mostrarModalHuella = true,
                huellaEnModoRegistro = true,
                huellaDniObjetivo = s.regDni,
                huellaSubtitulo = "Coloca tu dedo en el sensor para el DNI ${s.regDni}",
                huellaUsuarioObjetivo = null,
                huellaErrorInicial = null,
                errorMessage = null
            )
        }
    }

    fun iniciarCambioHuellaEnSesion() {
        val user = _state.value.usuarioAutenticado ?: return
        _state.update {
            it.copy(
                mostrarModalHuella = true,
                huellaEnModoRegistro = true,
                huellaDniObjetivo = user.dni,
                huellaSubtitulo = "Coloca tu nuevo dedo en el sensor para actualizar tu huella (${user.nombre})",
                huellaUsuarioObjetivo = user,
                huellaErrorInicial = null,
                errorMessage = null
            )
        }
    }

    fun cerrarModalHuella() {
        _state.update { it.copy(mostrarModalHuella = false, huellaErrorInicial = null) }
    }

    fun confirmarHuellaVerificada(context: Context, token: String) {
        val s = _state.value
        viewModelScope.launch {
            delay(720)
            if (s.vistaActual == BiometricVista.MARKETPLACE && s.usuarioAutenticado != null) {
                val user = s.usuarioAutenticado
                val payload = UsuarioUpsertDto(
                    dni = user.dni,
                    nombre = user.nombre,
                    correo = user.correo.ifBlank { "${user.dni}@emprendepiura.pe" },
                    telefono = user.telefono,
                    passwordHash = user.passwordHash,
                    zonaPiura = user.zonaPiura,
                    huella = token,
                    imagenFacial = user.imagenFacial,
                    vectorFacial = user.vectorFacial,
                    avatarUrl = user.fotoPerfil
                )
                try {
                    val actualizado = withContext(Dispatchers.IO) {
                        repository.insertOrUpdate(context, payload)
                    }
                    _state.update {
                        it.copy(
                            mostrarModalHuella = false,
                            usuarioAutenticado = actualizado,
                            errorMessage = null,
                            successMessage = "Huella dactilar actualizada correctamente."
                        )
                    }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            mostrarModalHuella = false,
                            errorMessage = BiometricValidator.mapearError(e, user.dni)
                        )
                    }
                }
            } else if (s.huellaEnModoRegistro) {
                _state.update {
                    it.copy(
                        mostrarModalHuella = false,
                        regHuellaToken = token,
                        errorMessage = null,
                        successMessage = "Huella dactilar registrada correctamente."
                    )
                }
            } else {
                val usuario = s.huellaUsuarioObjetivo ?: return@launch
                repository.recordarUltimoDni(context, usuario.dni)
                _state.update {
                    it.copy(
                        mostrarModalHuella = false,
                        mostrarCampoContrasena = false,
                        usuarioAutenticado = usuario,
                        metodoAutenticacion = "Verificado por Huella Dactilar",
                        vistaActual = BiometricVista.MARKETPLACE,
                        errorMessage = null,
                        successMessage = "Identidad verificada por huella. Bienvenido, ${usuario.nombre}."
                    )
                }
            }
        }
    }

    fun prepararLoginFacial(context: Context, onListoParaCamara: () -> Unit) {
        val dni = validarDniBienvenida() ?: return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, successMessage = null) }
            val usuario = withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, dni) }.getOrNull()
            }
            _state.update { it.copy(loading = false) }

            if (usuario == null) {
                _state.update {
                    it.copy(errorMessage = "El DNI $dni no se encuentra registrado. Usa Regístrate.")
                }
                return@launch
            }

            if (usuario.vectorFacial.isBlank()) {
                _state.update {
                    it.copy(
                        errorMessage = "Esta cuenta aún no tiene un rostro registrado. Ingresa con Contraseña o regístralo en tu Perfil."
                    )
                }
                return@launch
            }

            usuarioObjetivoFacial = usuario
            similitudFacialTemp = 0
            _state.update {
                it.copy(facialSubtituloTitular = "Titular: ${usuario.nombre} (${usuario.dni})")
            }
            onListoParaCamara()
        }
    }

    fun mostrarModalEscanerFacial(esModoRegistro: Boolean) {
        _state.update {
            it.copy(
                mostrarEscanerFacial = true,
                escanerEnModoRegistro = esModoRegistro,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun prepararRegistroFacial(onListoParaCamara: () -> Unit) {
        val s = _state.value
        if (BiometricValidator.validarDni(s.regDni) != null) {
            _state.update {
                it.copy(
                    errorMessage = "Ingresa primero los 8 dígitos del DNI antes de escanear tu rostro.",
                    successMessage = null
                )
            }
            return
        }
        onListoParaCamara()
    }

    fun iniciarRecapturaRostroEnSesion(onListoParaCamara: () -> Unit) {
        val user = _state.value.usuarioAutenticado ?: return
        _state.update {
            it.copy(
                regDni = user.dni,
                regNombre = user.nombre,
                facialSubtituloTitular = "Recapturar rostro: ${user.nombre} (${user.dni})"
            )
        }
        onListoParaCamara()
    }

    fun cerrarEscanerFacial() {
        _state.update {
            if (it.vistaActual == BiometricVista.MARKETPLACE) {
                it.copy(
                    mostrarEscanerFacial = false,
                    procesandoRostro = false,
                    regDni = "",
                    regNombre = "",
                    regFotoFacialUri = null,
                    regFotoFacialBytes = null,
                    regVectorFacial = ""
                )
            } else {
                it.copy(mostrarEscanerFacial = false, procesandoRostro = false)
            }
        }
    }

    suspend fun evaluarEscaneoFacialEnVivo(
        context: Context,
        vectorPromedio: String,
        frameBitmap: Bitmap
    ): ResultadoEscaneoEnVivo {
        if (_state.value.escanerEnModoRegistro) {
            val (uri, bytes) = withContext(Dispatchers.IO) {
                val u = FaceAnalyzerUtil.guardarBitmapTemporal(context, frameBitmap)
                val b = FaceAnalyzerUtil.comprimirBitmapJpeg(frameBitmap)
                u to b
            }
            _state.update {
                it.copy(
                    regFotoFacialUri = uri,
                    regFotoFacialBytes = bytes,
                    regVectorFacial = vectorPromedio
                )
            }
            return ResultadoEscaneoEnVivo.Exito("Rostro registrado correctamente")
        }

        val titular = usuarioObjetivoFacial ?: withContext(Dispatchers.IO) {
            runCatching { repository.getByDni(context, _state.value.loginDni) }.getOrNull()
        } ?: return ResultadoEscaneoEnVivo.Error("No se encontró el titular del DNI ingresado.")

        val comparacion = withContext(Dispatchers.Default) {
            FaceComparisonEngine.compararRostros(
                vectorRegistrado = titular.vectorFacial,
                vectorActual = vectorPromedio
            )
        }

        return if (comparacion.coincide) {
            usuarioObjetivoFacial = titular
            similitudFacialTemp = comparacion.porcentajeSimilitud
            repository.recordarUltimoDni(context, titular.dni)
            ResultadoEscaneoEnVivo.Exito(
                "Rostro verificado: ${titular.nombre} (${comparacion.porcentajeSimilitud}%)"
            )
        } else {
            ResultadoEscaneoEnVivo.Error(
                "El rostro no coincide con el titular registrado (${comparacion.porcentajeSimilitud}% similitud). Acceso denegado."
            )
        }
    }

    fun finalizarEscaneoFacialExitoso(context: Context? = null) {
        val s = _state.value
        if (s.vistaActual == BiometricVista.MARKETPLACE && s.usuarioAutenticado != null) {
            val user = s.usuarioAutenticado
            val bytes = s.regFotoFacialBytes
            val vec = s.regVectorFacial
            if (context != null && bytes != null && bytes.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        val nuevaUrl = withContext(Dispatchers.IO) {
                            repository.uploadFacePhoto(context, user.dni, bytes)
                        }
                        val payload = UsuarioUpsertDto(
                            dni = user.dni,
                            nombre = user.nombre,
                            correo = user.correo.ifBlank { "${user.dni}@emprendepiura.pe" },
                            telefono = user.telefono,
                            passwordHash = user.passwordHash,
                            zonaPiura = user.zonaPiura,
                            huella = user.huella,
                            imagenFacial = nuevaUrl,
                            vectorFacial = vec,
                            avatarUrl = nuevaUrl
                        )
                        val actualizado = withContext(Dispatchers.IO) {
                            repository.insertOrUpdate(context, payload)
                        }
                        _state.update {
                            it.copy(
                                mostrarEscanerFacial = false,
                                procesandoRostro = false,
                                usuarioAutenticado = actualizado,
                                regDni = "",
                                regNombre = "",
                                regFotoFacialUri = null,
                                regFotoFacialBytes = null,
                                regVectorFacial = "",
                                successMessage = "Rostro facial actualizado correctamente."
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                mostrarEscanerFacial = false,
                                procesandoRostro = false,
                                regDni = "",
                                regNombre = "",
                                regFotoFacialUri = null,
                                regFotoFacialBytes = null,
                                regVectorFacial = "",
                                errorMessage = BiometricValidator.mapearError(e, user.dni)
                            )
                        }
                    }
                }
                return
            }
        }

        if (s.escanerEnModoRegistro) {
            _state.update {
                it.copy(
                    mostrarEscanerFacial = false,
                    procesandoRostro = false,
                    successMessage = "Rostro capturado y verificado con éxito."
                )
            }
        } else {
            val usuario = usuarioObjetivoFacial ?: return
            val porcentaje = similitudFacialTemp
            _state.update {
                it.copy(
                    mostrarEscanerFacial = false,
                    mostrarCampoContrasena = false,
                    procesandoRostro = false,
                    usuarioAutenticado = usuario,
                    metodoAutenticacion = "Verificado por Reconocimiento Facial ($porcentaje%)",
                    vistaActual = BiometricVista.MARKETPLACE,
                    successMessage = "Reconocimiento facial exitoso ($porcentaje%). Bienvenido, ${usuario.nombre}."
                )
            }
        }
    }

    fun guardarUsuario(context: Context) {
        val s = _state.value
        val error = BiometricValidator.validarRegistro(
            dni = s.regDni,
            nombre = s.regNombre,
            telefono = s.regTelefono,
            correo = s.regCorreo,
            contrasena = s.regContrasena,
            huellaToken = s.regHuellaToken,
            rostroListo = s.rostroRegistroListo,
            esModoEdicion = s.esModoEdicion
        )
        if (error != null) {
            _state.update { it.copy(errorMessage = error, successMessage = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null, successMessage = null) }
            try {
                val cleanDni = s.regDni.trim()
                val fotoUrl = if (s.regFotoFacialBytes != null && s.regFotoFacialBytes.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.uploadFacePhoto(context, cleanDni, s.regFotoFacialBytes)
                    }
                } else {
                    s.regFotoFacialUri?.toString().orEmpty()
                }

                val passwordHashFinal = if (s.regContrasena.isNotBlank()) {
                    HashUtil.sha256Password(s.regContrasena)
                } else {
                    s.regPasswordHashActual
                }

                val correoFinal = s.regCorreo.trim().ifBlank { "${cleanDni}@emprendepiura.pe" }

                val payload = UsuarioUpsertDto(
                    dni = cleanDni,
                    nombre = s.regNombre.trim(),
                    correo = correoFinal,
                    telefono = s.regTelefono.trim(),
                    passwordHash = passwordHashFinal,
                    zonaPiura = s.regZonaPiura,
                    huella = s.regHuellaToken,
                    imagenFacial = fotoUrl,
                    vectorFacial = s.regVectorFacial,
                    avatarUrl = fotoUrl
                )

                val guardado = withContext(Dispatchers.IO) {
                    repository.insertOrUpdate(context, payload)
                }

                val mensajeExito = if (s.esModoEdicion) {
                    "Datos actualizados correctamente para \"${guardado.nombre}\"."
                } else {
                    "Cuenta de \"${guardado.nombre}\" creada correctamente."
                }

                val destino = if (s.usuarioAutenticado != null && s.usuarioAutenticado.dni == guardado.dni) {
                    BiometricVista.MARKETPLACE
                } else {
                    BiometricVista.BIENVENIDA
                }

                _state.update {
                    it.copy(
                        saving = false,
                        vistaActual = destino,
                        loginDni = guardado.dni,
                        loginContrasena = "",
                        mostrarCampoContrasena = false,
                        usuarioAutenticado = if (it.usuarioAutenticado?.dni == guardado.dni) guardado else it.usuarioAutenticado,
                        esModoEdicion = false,
                        buscandoUsuarioDni = false,
                        regDni = "",
                        regNombre = "",
                        regTelefono = "",
                        regCorreo = "",
                        regContrasena = "",
                        regPasswordHashActual = "",
                        regHuellaToken = "",
                        regFotoFacialUri = null,
                        regFotoFacialBytes = null,
                        regVectorFacial = "",
                        successMessage = mensajeExito
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, errorMessage = BiometricValidator.mapearError(e, s.regDni))
                }
            }
        }
    }

    fun mostrarError(mensaje: String) {
        _state.update { it.copy(errorMessage = mensaje, successMessage = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
