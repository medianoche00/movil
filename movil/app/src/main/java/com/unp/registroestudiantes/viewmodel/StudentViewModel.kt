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

data class UiState(
    val students: List<Student> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loadError: String? = null,
    val actionError: String? = null
)

/** Datos que entrega el formulario al guardar. */
class FormInput(
    val name: String,
    val age: Int,
    val career: String,
    val email: String?,
    val photoBytes: ByteArray?,   // null = no cambió la foto
    val cvUri: Uri?               // null = no se adjuntó/cambió el CV
)

class StudentViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StudentRepository()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            try {
                val list = repo.getAll()
                _state.update { it.copy(students = list, loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = e.message ?: "No se pudo conectar") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(actionError = null) }

    /** Crea o edita: sube archivos, analiza el CV, genera la reseña y guarda en Supabase. */
    fun save(existingId: String?, input: FormInput, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val started = System.currentTimeMillis()
            try {
                val app = getApplication<Application>()
                val existing = _state.value.students.firstOrNull { it.id == existingId }

                // 1) CV: extraer texto + subir archivo
                var cvText = existing?.backgroundText
                var cvUrl = existing?.cvUrl
                if (input.cvUri != null) {
                    cvText = CvTextExtractor.extract(app, input.cvUri)
                    val isPdf = app.contentResolver.getType(input.cvUri) == "application/pdf"
                    val bytes = withContext(Dispatchers.IO) {
                        app.contentResolver.openInputStream(input.cvUri)!!.use { it.readBytes() }
                    }
                    cvUrl = repo.uploadFile(Supa.CVS_BUCKET, "${UUID.randomUUID()}.${if (isPdf) "pdf" else "jpg"}", bytes)
                }

                // 2) Foto
                var photoUrl = existing?.photoUrl
                if (input.photoBytes != null) {
                    photoUrl = repo.uploadFile(Supa.PHOTOS_BUCKET, "${UUID.randomUUID()}.jpg", input.photoBytes)
                }

                // 3) Análisis local + reseña
                val found = CvAnalyzer.analyze(cvText.orEmpty())
                val review = CvAnalyzer.buildReview(input.name, input.age, input.career, found, cvUrl != null)
                val skillIds = repo.ensureSkills(found)

                // 4) Guardar en Supabase
                val data = StudentWrite(
                    name = input.name, age = input.age, career = input.career, email = input.email,
                    photoUrl = photoUrl, cvUrl = cvUrl, backgroundText = cvText, review = review
                )
                val saved = if (existingId == null) repo.create(data, skillIds)
                            else repo.update(existingId, data, skillIds)

                // Mínimo ~1.5 s para que se aprecie el shimmer de "generando reseña"
                val elapsed = System.currentTimeMillis() - started
                if (elapsed < 1500) delay(1500 - elapsed)

                _state.update { s ->
                    val list = if (existingId == null) listOf(saved) + s.students
                               else s.students.map { if (it.id == saved.id) saved else it }
                    s.copy(students = list, saving = false)
                }
                onDone(saved.id)
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, actionError = "No se pudo guardar: ${e.message}") }
            }
        }
    }

    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                _state.update { s -> s.copy(students = s.students.filterNot { it.id == id }) }
                onDone()
            } catch (e: Exception) {
                _state.update { it.copy(actionError = "No se pudo eliminar: ${e.message}") }
            }
        }
    }
}
