package com.unp.registroestudiantes.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unp.registroestudiantes.data.*
import com.unp.registroestudiantes.util.CvAnalyzer
import com.unp.registroestudiantes.util.CvTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/*
 * Archivo: StudentViewModel.kt
 * Proposito: Implementacion de la capa de presentacion con arquitectura MVVM (Model-View-ViewModel).
 *
 * Guia de transicion Java -> Kotlin:
 *
 * 1. Patron MVVM y AndroidViewModel:
 *    El ViewModel sobrevive a los cambios de configuracion del dispositivo (por ejemplo,
 *    cuando el usuario rota la pantalla de vertical a horizontal).
 *    En Java clasico, si rotabas la pantalla, la Activity se destruia y se perdian las
 *    variables, teniendo que lidiar con onSaveInstanceState(Bundle).
 *    Con ViewModel, el estado permanece intacto en memoria.
 *
 * 2. Estado reactivo con StateFlow vs LiveData:
 *    - MutableStateFlow: Almacena el estado actual y emite actualizaciones a quienes lo observan.
 *      Es similar a una variable con el patron Observer o a un store de Redux en React.
 *    - StateFlow (de solo lectura): Encapsula el estado para que la vista (pantallas) solo pueda
 *      LEER el estado, pero no modificarlo directamente.
 *    - Convencion de nombres (_state y state):
 *      '_state' (privado y mutable) y 'state' (publico e inmutable).
 *      En Java equivale a un campo privado con un getter publico que devuelve una copia inmutable.
 *
 * 3. Corrutinas con viewModelScope:
 *    'viewModelScope.launch { ... }' inicia una tarea asincrona vinculada al ciclo de vida
 *    del ViewModel. Si el ViewModel se destruye, todas sus tareas pendientes se cancelan
 *    automaticamente, evitando fugas de memoria (memory leaks).
 *
 * 4. Inmutabilidad con .copy():
 *    La data class UiState se actualiza mediante '_state.update { it.copy(...) }'.
 *    Crea una nueva instancia con solo los atributos especificados cambiados,
 *    evitando mutar objetos compartidos por referencia.
 *
 * 5. withContext(Dispatchers.IO):
 *    Cambia la ejecucion del codigo a un grupo de hilos optimizado para operaciones
 *    de entrada/salida (disco y red), garantizando que el hilo principal (Main Thread)
 *    de la interfaz de usuario se mantenga a 60/120 cuadros por segundo sin tirones.
 */

// ============================================================
// 1. Estado de la interfaz de usuario (UI State)
// ============================================================

/**
 * Representa todo lo que la pantalla necesita saber para dibujarse en cualquier momento.
 * En desarrollo web equivale al estado global de un componente React.
 */
data class UiState(
    val students: List<Student> = emptyList(), // Lista de estudiantes a renderizar
    val loading: Boolean = true,               // Indica si se esta cargando la lista inicial
    val saving: Boolean = false,               // Indica si se esta procesando un guardado
    val loadError: String? = null,             // Mensaje de error al cargar datos
    val actionError: String? = null            // Mensaje de error en operaciones (guardar/eliminar)
)

/**
 * Contenedor de datos que entrega el formulario de registro/edicion al ViewModel.
 */
class FormInput(
    val name: String,
    val age: Int,
    val career: String,
    val email: String?,
    val photoBytes: ByteArray?,   // null si el usuario no cambio ni selecciono foto
    val cvUri: Uri?               // null si el usuario no adjunto ni cambio el CV
)

// ============================================================
// 2. ViewModel
// ============================================================

class StudentViewModel(app: Application) : AndroidViewModel(app) {

    // Repositorio que ejecuta las operaciones contra Supabase
    private val repo = StudentRepository()

    // Estado interno mutable
    private val _state = MutableStateFlow(UiState())

    // Estado publico expuesto a las pantallas de Compose como flujo de solo lectura
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Bloque init: se ejecuta inmediatamente al instanciarse la clase (equivalente al constructor en Java)
    init {
        load()
    }

    /**
     * Carga o recarga la lista de estudiantes desde la base de datos.
     */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            try {
                val list = repo.getAll()
                _state.update { it.copy(students = list, loading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, loadError = e.message ?: "No se pudo conectar con el servidor")
                }
            }
        }
    }

    /** Limpia el mensaje de error de accion una vez mostrado al usuario. */
    fun clearError() {
        _state.update { it.copy(actionError = null) }
    }

    /**
     * Guarda un estudiante (creacion o edicion).
     * Secuencia de operaciones:
     *   1. Extrae el texto del CV (si se adjunto) con PDFBox o ML Kit OCR.
     *   2. Sube el archivo del CV a Supabase Storage.
     *   3. Sube la foto optimizada a Supabase Storage.
     *   4. Analiza el texto localmente para extraer aptitudes clave y armar la resena.
     *   5. Asegura que las aptitudes existan en la tabla 'skills'.
     *   6. Inserta o actualiza el registro en la tabla 'students'.
     *   7. Notifica a la UI mediante la funcion callback 'onDone'.
     */
    fun save(existingId: String?, input: FormInput, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val started = System.currentTimeMillis()

            try {
                val app = getApplication<Application>()
                val existing = _state.value.students.firstOrNull { it.id == existingId }

                // Paso 1 y 2: Procesamiento del CV
                var cvText = existing?.backgroundText
                var cvUrl = existing?.cvUrl
                if (input.cvUri != null) {
                    cvText = CvTextExtractor.extract(app, input.cvUri)
                    val isPdf = app.contentResolver.getType(input.cvUri) == "application/pdf"
                    val bytes = withContext(Dispatchers.IO) {
                        app.contentResolver.openInputStream(input.cvUri)!!.use { it.readBytes() }
                    }
                    val extension = if (isPdf) "pdf" else "jpg"
                    cvUrl = repo.uploadFile(Supa.CVS_BUCKET, "${UUID.randomUUID()}.$extension", bytes)
                }

                // Paso 3: Subida de la foto de perfil
                var photoUrl = existing?.photoUrl
                if (input.photoBytes != null) {
                    photoUrl = repo.uploadFile(Supa.PHOTOS_BUCKET, "${UUID.randomUUID()}.jpg", input.photoBytes)
                }

                // Paso 4: Analisis del texto y generacion automatica de la resena
                val found = CvAnalyzer.analyze(cvText.orEmpty())
                val review = CvAnalyzer.buildReview(input.name, input.age, input.career, found, cvUrl != null)
                val skillIds = repo.ensureSkills(found)

                // Paso 5 y 6: Guardado en base de datos
                val data = StudentWrite(
                    name = input.name,
                    age = input.age,
                    career = input.career,
                    email = input.email,
                    photoUrl = photoUrl,
                    cvUrl = cvUrl,
                    backgroundText = cvText,
                    review = review
                )
                val saved = if (existingId == null) repo.create(data, skillIds)
                else repo.update(existingId, data, skillIds)

                // Efecto visual: garantiza un tiempo minimo para que el usuario perciba la operacion
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < 1500) delay(1500 - elapsed)

                // Actualizacion del estado en memoria
                _state.update { s ->
                    val list = if (existingId == null) listOf(saved) + s.students
                    else s.students.map { if (it.id == saved.id) saved else it }
                    s.copy(students = list, saving = false)
                }

                // Ejecuta la navegacion al detalle con el ID guardado
                onDone(saved.id)
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, actionError = "No se pudo guardar: ${e.message}")
                }
            }
        }
    }

    /**
     * Elimina un estudiante de la base de datos y actualiza la lista local.
     */
    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _state.update { s ->
                    s.copy(students = s.students.filterNot { it.id == id })
                }
                onDone()
            } catch (e: Exception) {
                _state.update {
                    it.copy(actionError = "No se pudo eliminar: ${e.message}")
                }
            }
        }
    }
}
