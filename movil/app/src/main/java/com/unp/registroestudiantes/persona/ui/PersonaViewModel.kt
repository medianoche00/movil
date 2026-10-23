package com.unp.registroestudiantes.persona.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.registroestudiantes.persona.data.model.Persona
import com.unp.registroestudiantes.persona.data.model.PersonaWrite
import com.unp.registroestudiantes.persona.data.repository.PersonaRepository
import com.unp.registroestudiantes.persona.domain.PersonaValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Archivo: PersonaViewModel.kt
 * Proposito: ViewModel reactivo con StateFlow para el modulo de Registro de Personas (Trabajo N°03).
 */
class PersonaViewModel(
    private val repository: PersonaRepository = PersonaRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(PersonaUiState(loadingProfesiones = true))
    val state: StateFlow<PersonaUiState> = _state.asStateFlow()

    init {
        cargarProfesiones()
        cargarDirectorio()
    }

    /** Carga el catalogo de profesiones. */
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
                        errorMessage = "Error al cargar profesiones. Revise su conexión a internet."
                    )
                }
            }
        }
    }

    /** Carga la lista completa de personas registradas para el Directorio. */
    fun cargarDirectorio() {
        viewModelScope.launch {
            _state.update { it.copy(loadingDirectorio = true) }
            try {
                val lista = withContext(Dispatchers.IO) { repository.getAll() }
                _state.update { it.copy(directorio = lista, loadingDirectorio = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingDirectorio = false) }
            }
        }
    }

    // Navegación interna entre vistas (Formulario, Directorio, Detalle)
    fun abrirDirectorio() {
        cargarDirectorio()
        _state.update { it.copy(vistaActual = PersonaVista.DIRECTORIO) }
    }

    fun abrirDetalle(persona: Persona) {
        _state.update {
            it.copy(
                personaSeleccionadaDetalle = persona,
                vistaActual = PersonaVista.DETALLE
            )
        }
    }

    fun volverAlFormulario() {
        _state.update { it.copy(vistaActual = PersonaVista.FORMULARIO) }
    }

    fun modificarDesdeDetalle(persona: Persona) {
        _state.update {
            it.copy(
                vistaActual = PersonaVista.FORMULARIO,
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
                errorMessage = null,
                successMessage = "Datos de \"${persona.nombreCompleto}\" cargados en el formulario."
            )
        }
    }

    // Actualizaciones de campos reactivos
    fun onDniChange(v: String)            = _state.update { it.copy(dni = v.take(8)) }
    fun onNombresChange(v: String)        = _state.update { it.copy(nombres = v) }
    fun onApellidosChange(v: String)      = _state.update { it.copy(apellidos = v) }
    fun onDireccionChange(v: String)      = _state.update { it.copy(direccion = v) }
    fun onCelularChange(v: String)        = _state.update { it.copy(celular = v.take(9)) }
    fun onCorreoChange(v: String)         = _state.update { it.copy(correoElectronico = v) }
    fun onProfesionChange(codigo: String) = _state.update { it.copy(codigoProfesion = codigo) }
    fun onFotoCapturada(uri: Uri)         = _state.update { it.copy(fotoUri = uri) }

    /** Limpia los campos del formulario y restablece el modo a nuevo registro. */
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

    /** Busca una persona por su DNI. */
    fun buscarPorDni(dni: String) {
        val errorDni = PersonaValidator.validarDni(dni)
        if (errorDni != null) {
            _state.update { it.copy(errorMessage = errorDni) }
            return
        }
        val cleanDni = dni.trim()
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
                        errorMessage = "Error al buscar la persona. Intente nuevamente."
                    )
                }
            }
        }
    }

    /** Inserta una nueva persona junto con su fotografia si fue tomada. */
    fun guardar(context: Context, onSuccess: () -> Unit = {}) {
        val s = _state.value
        val error = PersonaValidator.validarFormulario(
            s.dni, s.nombres, s.apellidos, s.direccion, s.codigoProfesion, s.celular, s.correoElectronico
        )
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            try {
                val fotoUrl = subirFotoSiExiste(context, s)
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
                cargarDirectorio()
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = PersonaValidator.mapearError(e, "guardar", s.dni)
                    )
                }
            }
        }
    }

    /** Actualiza la persona actualmente cargada auditando los datos modificados. */
    fun actualizar(context: Context, onSuccess: () -> Unit = {}) {
        val s = _state.value
        if (s.personaActual == null) {
            _state.update { it.copy(errorMessage = "Primero busque una persona por DNI para actualizarla.") }
            return
        }
        val error = PersonaValidator.validarFormulario(
            s.dni, s.nombres, s.apellidos, s.direccion, s.codigoProfesion, s.celular, s.correoElectronico
        )
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorMessage = null) }
            try {
                val fotoUrl = subirFotoSiExiste(context, s)
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

                // Detección estética de cambios
                val cambios = mutableListOf<String>()
                if (s.celular.trim() != s.personaActual.celular) {
                    cambios.add("Celular: ${s.personaActual.celular} → ${s.celular.trim()}")
                }
                if (s.direccion.trim() != s.personaActual.direccion) {
                    cambios.add("Dirección: ${s.personaActual.direccion} → ${s.direccion.trim()}")
                }
                if (s.nombres.trim() != s.personaActual.nombres) {
                    cambios.add("Nombres: ${s.personaActual.nombres} → ${s.nombres.trim()}")
                }
                if (s.apellidos.trim() != s.personaActual.apellidos) {
                    cambios.add("Apellidos: ${s.personaActual.apellidos} → ${s.apellidos.trim()}")
                }
                if (s.codigoProfesion != s.personaActual.codigoProfesion) {
                    cambios.add("Profesión actualizada")
                }
                if (fotoUrl != null) {
                    cambios.add("Fotografía actualizada")
                }

                val mensajeActualizado = if (cambios.isEmpty()) {
                    "Datos de la persona confirmados sin cambios."
                } else {
                    "Actualizado: " + cambios.joinToString(" • ")
                }

                val actualizada = withContext(Dispatchers.IO) {
                    repository.update(s.personaActual.dni, data)
                }

                _state.update {
                    it.copy(
                        saving = false,
                        personaActual = actualizada,
                        fotoPath = actualizada.foto,
                        fotoUri = null,
                        successMessage = mensajeActualizado
                    )
                }
                cargarDirectorio()
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = PersonaValidator.mapearError(e, "actualizar", s.dni)
                    )
                }
            }
        }
    }

    /** Elimina la persona actualmente cargada. */
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
                nuevo()
                _state.update {
                    it.copy(
                        saving = false,
                        successMessage = "Persona \"$nombre\" eliminada exitosamente."
                    )
                }
                cargarDirectorio()
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = PersonaValidator.mapearError(e, "eliminar", s.personaActual.dni)
                    )
                }
            }
        }
    }

    /** Sube la fotografia nueva si existe en el estado local. */
    private suspend fun subirFotoSiExiste(context: Context, s: PersonaUiState): String? {
        val uri = s.fotoUri ?: return null
        return try {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return null
            withContext(Dispatchers.IO) {
                repository.uploadPhoto(s.dni.trim(), bytes)
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(errorMessage = "Advertencia: No se pudo procesar la fotografía. El registro continuará sin foto.")
            }
            null
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
