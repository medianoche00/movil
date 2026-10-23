package com.unp.registroestudiantes.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.registroestudiantes.data.Persona
import com.unp.registroestudiantes.data.PersonaRepository
import com.unp.registroestudiantes.data.PersonaWrite
import com.unp.registroestudiantes.data.Profesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Archivo: PersonaViewModel.kt
 * Proposito: ViewModel para el Trabajo N°03 - Registro de Personas.
 *
 * Gestiona el estado de la UI de forma reactiva usando StateFlow, y delega
 * todas las operaciones de base de datos y storage al PersonaRepository.
 *
 * Patron MVVM:
 *   UI (PersonaScreen) -> observa estado (StateFlow) -> emitido por PersonaViewModel
 *   UI llama metodos del VM -> VM delega al Repository -> Repository habla con Supabase
 */

// ============================================================
// Estado de la UI
// ============================================================

data class PersonaUiState(
    // Catálogo
    val profesiones: List<Profesion> = emptyList(),
    val loadingProfesiones: Boolean = false,

    // Formulario
    val dni: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val direccion: String = "",
    val codigoProfesion: String = "",
    val celular: String = "",
    val correoElectronico: String = "",
    val fotoUri: Uri? = null,           // URI temporal de la foto recién tomada (local)
    val fotoPath: String? = null,       // URL pública / path persistido en BD

    // Modos de operación
    val personaActual: Persona? = null, // null = modo NUEVO, no null = modo EDICION
    val loading: Boolean = false,
    val saving: Boolean = false,

    // Mensajes de feedback
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    /** true si hay una persona cargada para buscar/editar/eliminar */
    val modoEdicion: Boolean get() = personaActual != null
}

// ============================================================
// ViewModel
// ============================================================

class PersonaViewModel(
    private val repository: PersonaRepository = PersonaRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(PersonaUiState(loadingProfesiones = true))
    val state: StateFlow<PersonaUiState> = _state.asStateFlow()

    init {
        cargarProfesiones()
    }

    // ------------------------------------------------------------------
    // Carga inicial del catálogo de profesiones
    // ------------------------------------------------------------------

    fun cargarProfesiones() {
        viewModelScope.launch {
            _state.update { it.copy(loadingProfesiones = true, errorMessage = null) }
            try {
                val lista = withContext(Dispatchers.IO) { repository.getProfesiones() }
                _state.update { it.copy(profesiones = lista, loadingProfesiones = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingProfesiones = false,
                        errorMessage = "Error al cargar profesiones: ${e.localizedMessage ?: "Revise su conexión"}"
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Actualización reactiva de campos del formulario
    // ------------------------------------------------------------------

    fun onDniChange(v: String)              = _state.update { it.copy(dni = v.take(8)) }
    fun onNombresChange(v: String)          = _state.update { it.copy(nombres = v) }
    fun onApellidosChange(v: String)        = _state.update { it.copy(apellidos = v) }
    fun onDireccionChange(v: String)        = _state.update { it.copy(direccion = v) }
    fun onCelularChange(v: String)          = _state.update { it.copy(celular = v.take(9)) }
    fun onCorreoChange(v: String)           = _state.update { it.copy(correoElectronico = v) }
    fun onProfesionChange(codigo: String)   = _state.update { it.copy(codigoProfesion = codigo) }

    /** Persiste la URI local de la foto recién capturada */
    fun onFotoCapturada(uri: Uri) = _state.update { it.copy(fotoUri = uri) }

    // ------------------------------------------------------------------
    // Botón NUEVO: limpia el formulario
    // ------------------------------------------------------------------

    fun nuevo() {
        _state.update {
            it.copy(
                dni = "",
                nombres = "",
                apellidos = "",
                direccion = "",
                codigoProfesion = "",
                celular = "",
                correoElectronico = "",
                fotoUri = null,
                fotoPath = null,
                personaActual = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    // ------------------------------------------------------------------
    // Botón BUSCAR: busca por DNI
    // ------------------------------------------------------------------

    fun buscarPorDni(dni: String) {
        val cleanDni = dni.trim()
        if (cleanDni.length != 8 || !cleanDni.all { it.isDigit() }) {
            _state.update { it.copy(errorMessage = "El DNI debe tener exactamente 8 dígitos numéricos.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, successMessage = null) }
            try {
                val persona = withContext(Dispatchers.IO) { repository.getByDni(cleanDni) }
                if (persona != null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            personaActual = persona,
                            dni = persona.dni,
                            nombres = persona.nombres,
                            apellidos = persona.apellidos,
                            direccion = persona.direccion,
                            codigoProfesion = persona.codigoProfesion,
                            celular = persona.celular,
                            correoElectronico = persona.correoElectronico,
                            fotoUri = null,
                            fotoPath = persona.foto,
                            successMessage = "Persona encontrada."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            loading = false,
                            personaActual = null,
                            errorMessage = "No se encontró ninguna persona con DNI $cleanDni."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        errorMessage = "Error al buscar: ${e.localizedMessage ?: "Intente nuevamente"}"
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Botón GUARDAR: inserta un registro nuevo
    // ------------------------------------------------------------------

    fun guardar(context: Context, onSuccess: () -> Unit = {}) {
        val s = _state.value
        val errorValidacion = validar(s)
        if (errorValidacion != null) {
            _state.update { it.copy(errorMessage = errorValidacion) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            try {
                // 1. Si hay foto nueva capturada, subirla al bucket primero
                val fotoUrl = subirFotoSiHay(context, s)

                // 2. Insertar la persona con la URL de la foto
                val data = PersonaWrite(
                    dni = s.dni.trim(),
                    nombres = s.nombres.trim(),
                    apellidos = s.apellidos.trim(),
                    direccion = s.direccion.trim(),
                    codigoProfesion = s.codigoProfesion,
                    celular = s.celular.trim(),
                    correoElectronico = s.correoElectronico.trim(),
                    foto = fotoUrl
                )
                val creada = withContext(Dispatchers.IO) { repository.insert(data) }

                _state.update {
                    it.copy(
                        saving = false,
                        personaActual = creada,
                        fotoPath = creada.foto,
                        successMessage = "Persona \"${creada.nombreCompleto}\" guardada exitosamente."
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = buildFriendlyError(e, "guardar")
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Botón ACTUALIZAR: modifica la persona cargada
    // ------------------------------------------------------------------

    fun actualizar(context: Context, onSuccess: () -> Unit = {}) {
        val s = _state.value
        if (s.personaActual == null) {
            _state.update { it.copy(errorMessage = "Primero busque una persona por DNI para actualizarla.") }
            return
        }
        val errorValidacion = validar(s)
        if (errorValidacion != null) {
            _state.update { it.copy(errorMessage = errorValidacion) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            try {
                // 1. Si hay foto nueva, subirla (sobreescribe la anterior en el bucket)
                val fotoUrl = subirFotoSiHay(context, s)

                // 2. Actualizar registro en BD
                val data = PersonaWrite(
                    dni = s.dni.trim(),
                    nombres = s.nombres.trim(),
                    apellidos = s.apellidos.trim(),
                    direccion = s.direccion.trim(),
                    codigoProfesion = s.codigoProfesion,
                    celular = s.celular.trim(),
                    correoElectronico = s.correoElectronico.trim(),
                    foto = fotoUrl ?: s.fotoPath
                )
                val actualizada = withContext(Dispatchers.IO) {
                    repository.update(s.personaActual.dni, data)
                }

                _state.update {
                    it.copy(
                        saving = false,
                        personaActual = actualizada,
                        fotoPath = actualizada.foto,
                        fotoUri = null,
                        successMessage = "Persona \"${actualizada.nombreCompleto}\" actualizada correctamente."
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = buildFriendlyError(e, "actualizar")
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Botón ELIMINAR: borra la persona cargada
    // ------------------------------------------------------------------

    fun eliminar(onSuccess: () -> Unit = {}) {
        val s = _state.value
        if (s.personaActual == null) {
            _state.update { it.copy(errorMessage = "Primero busque una persona por DNI para eliminarla.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) { repository.delete(s.personaActual.dni) }
                val nombre = s.personaActual.nombreCompleto
                nuevo()   // Limpiar formulario tras eliminar
                _state.update {
                    it.copy(
                        saving = false,
                        successMessage = "Persona \"$nombre\" eliminada exitosamente."
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = buildFriendlyError(e, "eliminar")
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers privados
    // ------------------------------------------------------------------

    /** Valida los campos obligatorios del formulario. Devuelve null si es válido. */
    private fun validar(s: PersonaUiState): String? = when {
        s.dni.trim().length != 8 || !s.dni.trim().all { it.isDigit() } ->
            "El DNI debe tener exactamente 8 dígitos numéricos."
        s.nombres.isBlank() ->
            "El campo Nombres es obligatorio."
        s.apellidos.isBlank() ->
            "El campo Apellidos es obligatorio."
        s.direccion.isBlank() ->
            "El campo Dirección es obligatorio."
        s.codigoProfesion.isBlank() ->
            "Debe seleccionar una profesión."
        s.celular.trim().length != 9 || !s.celular.trim().all { it.isDigit() } ->
            "El celular debe tener exactamente 9 dígitos numéricos."
        s.correoElectronico.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(s.correoElectronico.trim()).matches() ->
            "Ingrese un correo electrónico válido."
        else -> null
    }

    /**
     * Si el estado tiene una URI de foto nueva capturada, lee sus bytes y la sube al bucket.
     * @return URL pública de la foto en Supabase Storage, o null si no hay foto nueva.
     */
    private suspend fun subirFotoSiHay(context: Context, s: PersonaUiState): String? {
        val uri = s.fotoUri ?: return null
        return try {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return null
            withContext(Dispatchers.IO) {
                repository.uploadPhoto(s.dni.trim(), bytes)
            }
        } catch (e: Exception) {
            // Si falla la subida de la foto, no bloqueamos el guardado pero notificamos
            _state.update {
                it.copy(
                    errorMessage = "Advertencia: No se pudo subir la foto. ${e.localizedMessage}"
                )
            }
            null
        }
    }

    /** Construye mensajes de error amigables según el mensaje raw de Supabase. */
    private fun buildFriendlyError(e: Exception, operacion: String): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("chk_dni_format", ignoreCase = true) ->
                "El DNI debe ser exactamente 8 dígitos numéricos."
            raw.contains("chk_celular_format", ignoreCase = true) ->
                "El celular debe ser exactamente 9 dígitos numéricos."
            raw.contains("fk_persona_profesion", ignoreCase = true) ->
                "La profesión seleccionada no existe en el catálogo."
            raw.contains("duplicate", ignoreCase = true) || raw.contains("unique", ignoreCase = true) ->
                "Ya existe una persona registrada con el DNI ${_state.value.dni}."
            else ->
                "Error al $operacion: ${e.localizedMessage ?: "Intente nuevamente"}"
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
