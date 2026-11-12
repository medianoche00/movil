package com.unp.registroestudiantes.biometria.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometrico
import com.unp.registroestudiantes.biometria.data.model.UsuarioBiometricoWrite
import com.unp.registroestudiantes.biometria.data.repository.BiometricRepository
import com.unp.registroestudiantes.biometria.domain.BiometricValidator
import com.unp.registroestudiantes.biometria.domain.FaceComparisonEngine
import com.unp.registroestudiantes.biometria.ui.components.ResultadoEscaneoEnVivo
import com.unp.registroestudiantes.biometria.util.FaceAnalyzerUtil
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
 * Proposito: ViewModel del Trabajo N°04 (Rediseño v4 · Modo A) con validacion de DNI en pantalla
 *            principal, verificacion biometrica 1:1 contra el titular del DNI (Huella y Rostro)
 *            y despliegue inline de Contrasena.
 */
class BiometricViewModel(
    private val repository: BiometricRepository = BiometricRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricUiState())
    val state: StateFlow<BiometricUiState> = _state.asStateFlow()

    private var dniInicializado = false
    private var usuarioObjetivoFacial: UsuarioBiometrico? = null
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

    // Actualizadores de campos en Bienvenida
    fun onLoginDniChange(v: String) {
        val soloDigitos = v.filter { it.isDigit() }.take(8)
        _state.update {
            it.copy(
                loginDni = soloDigitos,
                dniInlineError = null
            )
        }
    }

    fun onLoginContrasenaChange(v: String) {
        _state.update { it.copy(loginContrasena = v) }
    }

    fun ocultarContrasenaInline() {
        _state.update { it.copy(mostrarCampoContrasena = false, loginContrasena = "") }
    }

    // Actualizadores de campos en Registro y Modificacion
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

    fun onRegNombreChange(v: String) {
        _state.update { it.copy(regNombre = v) }
    }

    fun onRegContrasenaChange(v: String) {
        _state.update { it.copy(regContrasena = v) }
    }

    /** Busca si un DNI ya esta registrado y precarga sus datos para permitir su edicion */
    fun buscarYPrecargarUsuario(context: Context, dni: String) {
        if (dni.length != 8) return
        viewModelScope.launch {
            _state.update { it.copy(buscandoUsuarioDni = true) }
            val usuario = withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, dni) }.getOrNull()
            }
            _state.update { it.copy(buscandoUsuarioDni = false) }

            if (usuario != null) {
                val fotoUri = if (usuario.imagenFacial.isNotBlank()) {
                    runCatching { Uri.parse(usuario.imagenFacial) }.getOrNull()
                } else null

                _state.update {
                    it.copy(
                        regDni = usuario.dni,
                        regNombre = usuario.nombre,
                        regContrasena = usuario.contrasena,
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
                _state.update { it.copy(esModoEdicion = false) }
            }
        }
    }

    // Navegacion entre pantallas
    fun irARegistro(context: Context? = null) {
        val dniActual = _state.value.loginDni.trim()
        _state.update {
            it.copy(
                vistaActual = BiometricVista.REGISTRO,
                regDni = if (dniActual.length == 8) dniActual else it.regDni,
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
            val fotoUri = if (user.imagenFacial.isNotBlank()) {
                runCatching { Uri.parse(user.imagenFacial) }.getOrNull()
            } else null

            _state.update {
                it.copy(
                    vistaActual = BiometricVista.REGISTRO,
                    regDni = user.dni,
                    regNombre = user.nombre,
                    regContrasena = user.contrasena,
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
        val dni = _state.value.regDni
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
                regContrasena = "",
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
                errorMessage = null,
                successMessage = "Sesión cerrada correctamente."
            )
        }
    }

    /** Valida que el DNI de Bienvenida tenga 8 digitos; si no, sacude el campo y muestra aviso inline. */
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

    /** 1. Despliega el campo de contrasena inline tras validar que el DNI este registrado. */
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

            _state.update {
                it.copy(
                    mostrarCampoContrasena = true,
                    errorMessage = null
                )
            }
        }
    }

    /** Confirma el acceso con el DNI y la Contrasena ingresada en el bloque inline. */
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
                val usuario = withContext(Dispatchers.IO) {
                    repository.getByDni(context, dni)
                }
                if (usuario == null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            errorMessage = "El DNI $dni no se encuentra registrado. Usa Regístrate."
                        )
                    }
                    return@launch
                }
                if (usuario.contrasena != pass) {
                    _state.update {
                        it.copy(
                            loading = false,
                            errorMessage = "La contraseña ingresada es incorrecta."
                        )
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
                        vistaActual = BiometricVista.POST_LOGIN,
                        successMessage = "Bienvenido, ${usuario.nombre}."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        errorMessage = BiometricValidator.mapearError(e, dni)
                    )
                }
            }
        }
    }

    /** 2. Valida el DNI y abre el modal de Huella para ese titular especifico. */
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
                        huellaErrorInicial = "Esta cuenta aún no tiene una huella registrada."
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

    /** Abre el modal de huella durante el registro de un nuevo usuario. */
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

    /** Abre el modal para modificar o registrar una nueva huella digital desde la sesion iniciada */
    fun iniciarCambioHuellaPostLogin() {
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
        _state.update {
            it.copy(
                mostrarModalHuella = false,
                huellaErrorInicial = null
            )
        }
    }

    fun confirmarHuellaVerificada(context: Context, token: String) {
        val s = _state.value
        viewModelScope.launch {
            delay(720)
            if (s.vistaActual == BiometricVista.POST_LOGIN && s.usuarioAutenticado != null) {
                // Actualiza la huella directamente para el usuario en sesion activa
                val user = s.usuarioAutenticado
                val payload = UsuarioBiometricoWrite(
                    dni = user.dni,
                    nombre = user.nombre,
                    contrasena = user.contrasena,
                    huella = token,
                    imagenFacial = user.imagenFacial,
                    vectorFacial = user.vectorFacial
                )
                try {
                    val actualizado = withContext(Dispatchers.IO) {
                        repository.update(context, payload)
                    }
                    _state.update {
                        it.copy(
                            mostrarModalHuella = false,
                            usuarioAutenticado = actualizado,
                            errorMessage = null,
                            successMessage = "Nueva huella dactilar registrada y actualizada correctamente."
                        )
                    }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            mostrarModalHuella = false,
                            errorMessage = "No se pudo actualizar la huella dactilar: ${e.message}"
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
                        vistaActual = BiometricVista.POST_LOGIN,
                        errorMessage = null,
                        successMessage = "Identidad verificada por huella. Bienvenido, ${usuario.nombre}."
                    )
                }
            }
        }
    }

    /** 3. Valida que el DNI exista antes de solicitar camara y abrir el escaner facial 1:1. */
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

            usuarioObjetivoFacial = usuario
            similitudFacialTemp = 0
            _state.update {
                it.copy(
                    facialSubtituloTitular = "Titular: ${usuario.nombre} (${usuario.dni})"
                )
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

    /** Valida el DNI en Registro antes de solicitar camara para capturar el rostro. */
    fun prepararRegistroFacial(onListoParaCamara: () -> Unit) {
        val s = _state.value
        val errorDni = BiometricValidator.validarDni(s.regDni)
        if (errorDni != null) {
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

    fun cerrarEscanerFacial() {
        _state.update {
            it.copy(
                mostrarEscanerFacial = false,
                procesandoRostro = false
            )
        }
    }

    /**
     * Evalua el descriptor de 232 dimensiones promediado tras 6 frames estables en CameraX.
     * En Login realiza comparacion estricta 1:1 exclusivamente contra el titular del DNI ingresado.
     */
    suspend fun evaluarEscaneoFacialEnVivo(
        context: Context,
        vectorPromedio: String,
        frameBitmap: Bitmap
    ): ResultadoEscaneoEnVivo {
        val enModoRegistro = _state.value.escanerEnModoRegistro

        if (enModoRegistro) {
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
        } else {
            val titular = usuarioObjetivoFacial ?: withContext(Dispatchers.IO) {
                runCatching { repository.getByDni(context, _state.value.loginDni) }.getOrNull()
            } ?: return ResultadoEscaneoEnVivo.Error(
                "No se encontró el titular del DNI ingresado."
            )

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
                    "El rostro presentado no coincide con el titular registrado (${comparacion.porcentajeSimilitud}% similitud). Acceso denegado."
                )
            }
        }
    }

    /** Inicia la recaptura del rostro facial directamente desde la sesion iniciada */
    fun iniciarRecapturaRostroPostLogin(onListoParaCamara: () -> Unit) {
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

    /** Se ejecuta tras mostrarse el recuadro verde de confirmacion en el escaner facial. */
    fun finalizarEscaneoFacialExitoso(context: Context? = null) {
        val s = _state.value
        if (s.vistaActual == BiometricVista.POST_LOGIN && s.usuarioAutenticado != null) {
            val user = s.usuarioAutenticado
            val bytes = s.regFotoFacialBytes
            val vec = s.regVectorFacial
            if (context != null && bytes != null && bytes.isNotEmpty()) {
                viewModelScope.launch {
                    try {
                        val nuevaUrl = withContext(Dispatchers.IO) {
                            repository.uploadFacePhoto(context, user.dni, bytes)
                        }
                        val payload = UsuarioBiometricoWrite(
                            dni = user.dni,
                            nombre = user.nombre,
                            contrasena = user.contrasena,
                            huella = user.huella,
                            imagenFacial = nuevaUrl,
                            vectorFacial = vec
                        )
                        val actualizado = withContext(Dispatchers.IO) {
                            repository.update(context, payload)
                        }
                        _state.update {
                            it.copy(
                                mostrarEscanerFacial = false,
                                procesandoRostro = false,
                                usuarioAutenticado = actualizado,
                                successMessage = "Rostro facial recapturado y actualizado correctamente."
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                mostrarEscanerFacial = false,
                                procesandoRostro = false,
                                errorMessage = "No se pudo actualizar el rostro: ${e.message}"
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
                    vistaActual = BiometricVista.POST_LOGIN,
                    successMessage = "Reconocimiento facial exitoso ($porcentaje%). Bienvenido, ${usuario.nombre}."
                )
            }
        }
    }

    /** Guarda o actualiza el usuario con su DNI, Nombre, Contrasena, Huella y Rostro. */
    fun guardarUsuario(context: Context) {
        val s = _state.value
        val error = BiometricValidator.validarRegistro(
            dni = s.regDni,
            nombre = s.regNombre,
            contrasena = s.regContrasena,
            huellaToken = s.regHuellaToken,
            rostroListo = s.rostroRegistroListo
        )
        if (error != null) {
            _state.update { it.copy(errorMessage = error, successMessage = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null, successMessage = null) }
            try {
                // Subir fotografia solo si fue capturada nuevamente; si no, conservar la existente
                val fotoUrl = if (s.regFotoFacialBytes != null && s.regFotoFacialBytes.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.uploadFacePhoto(context, s.regDni, s.regFotoFacialBytes)
                    }
                } else {
                    s.regFotoFacialUri?.toString().orEmpty()
                }

                val payload = UsuarioBiometricoWrite(
                    dni = s.regDni.trim(),
                    nombre = s.regNombre.trim(),
                    contrasena = s.regContrasena,
                    huella = s.regHuellaToken,
                    imagenFacial = fotoUrl,
                    vectorFacial = s.regVectorFacial
                )

                val creado = withContext(Dispatchers.IO) {
                    repository.insert(context, payload)
                }

                val mensajeExito = if (s.esModoEdicion) {
                    "Datos biométricos actualizados correctamente para \"${creado.nombre}\"."
                } else {
                    "Cuenta de \"${creado.nombre}\" creada correctamente."
                }

                val destino = if (s.usuarioAutenticado != null && s.usuarioAutenticado.dni == creado.dni) {
                    BiometricVista.POST_LOGIN
                } else {
                    BiometricVista.BIENVENIDA
                }

                _state.update {
                    it.copy(
                        saving = false,
                        vistaActual = destino,
                        loginDni = creado.dni,
                        loginContrasena = "",
                        mostrarCampoContrasena = false,
                        usuarioAutenticado = if (it.usuarioAutenticado != null && it.usuarioAutenticado.dni == creado.dni) creado else it.usuarioAutenticado,
                        esModoEdicion = false,
                        buscandoUsuarioDni = false,
                        regDni = "",
                        regNombre = "",
                        regContrasena = "",
                        regHuellaToken = "",
                        regFotoFacialUri = null,
                        regFotoFacialBytes = null,
                        regVectorFacial = "",
                        successMessage = mensajeExito
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = BiometricValidator.mapearError(e, s.regDni)
                    )
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
