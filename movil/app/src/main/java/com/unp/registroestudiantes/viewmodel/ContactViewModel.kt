package com.unp.registroestudiantes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unp.registroestudiantes.data.Contact
import com.unp.registroestudiantes.data.ContactInsert
import com.unp.registroestudiantes.data.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Estado general de la UI para la pantalla de Contactos.
 */
data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel que gestiona la agenda de contactos conectada a Supabase.
 * Provee operaciones reactivas optimistas para listar, agregar y eliminar contactos al instante.
 */
class ContactViewModel(
    private val repository: ContactRepository = ContactRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState(loading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    // Flujo sincronizado reactivamente para MenuScreen y navegacion
    val contacts: StateFlow<List<Contact>> = _uiState
        .map { it.contacts }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        loadContacts()
    }

    /**
     * Carga todos los contactos directamente desde la tabla 'contacts' en Supabase.
     */
    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            try {
                val list = withContext(Dispatchers.IO) {
                    repository.getAll()
                }
                _uiState.update { it.copy(contacts = list, loading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = "Error al cargar contactos: ${e.localizedMessage ?: "Revise su conexión a Internet"}"
                    )
                }
            }
        }
    }

    /**
     * Registra un nuevo contacto en Supabase con actualizacion optimista e instantanea en la UI.
     */
    fun addContact(
        firstName: String,
        lastName: String,
        username: String?,
        countryCode: String,
        phone: String,
        email: String?,
        syncToPhone: Boolean,
        onImmediateSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanFirstName = firstName.trim()
        val cleanLastName = lastName.trim()
        val cleanUsername = username?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
        val cleanCountryCode = countryCode.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email?.trim()?.takeIf { it.isNotBlank() }

        // Validacion 1: Comprobar duplicidad de nombre y apellido (insensible a mayusculas/minusculas)
        val currentList = _uiState.value.contacts
        val duplicateNameContact = currentList.firstOrNull {
            it.firstName.trim().equals(cleanFirstName, ignoreCase = true) &&
            it.lastName.trim().equals(cleanLastName, ignoreCase = true)
        }

        if (duplicateNameContact != null) {
            val errorText = if (duplicateNameContact.phone.trim() == cleanPhone) {
                "Ya existe un contacto registrado con este mismo nombre y número de teléfono."
            } else {
                "Ya existe un contacto registrado con el nombre \"${duplicateNameContact.fullName}\". Por favor utilice otro nombre o agregue un distintivo."
            }
            _uiState.update { it.copy(errorMessage = errorText) }
            onError(errorText)
            return
        }

        // Validacion 2: Comprobar duplicidad de numero telefonico
        val duplicatePhoneContact = currentList.firstOrNull {
            it.phone.trim() == cleanPhone && it.countryCode.trim() == cleanCountryCode
        }
        if (duplicatePhoneContact != null) {
            val errorText = "El número $cleanCountryCode $cleanPhone ya pertenece al contacto \"${duplicatePhoneContact.fullName}\"."
            _uiState.update { it.copy(errorMessage = errorText) }
            onError(errorText)
            return
        }

        // Validacion 3: Comprobar duplicidad de nombre de usuario si se ingreso
        if (cleanUsername != null) {
            val duplicateUserContact = currentList.firstOrNull {
                it.username?.trim()?.removePrefix("@")?.equals(cleanUsername, ignoreCase = true) == true
            }
            if (duplicateUserContact != null) {
                val errorText = "El nombre de usuario \"@$cleanUsername\" ya está registrado. Por favor elija otro."
                _uiState.update { it.copy(errorMessage = errorText) }
                onError(errorText)
                return
            }
        }

        // 1. Crear un contacto provisional para reflejarlo de inmediato en la lista (0ms de retraso)
        val tempId = "temp_${UUID.randomUUID()}"
        val tempContact = Contact(
            id = tempId,
            firstName = cleanFirstName,
            lastName = cleanLastName,
            username = cleanUsername,
            countryCode = cleanCountryCode,
            phone = cleanPhone,
            email = cleanEmail,
            syncToPhone = syncToPhone
        )

        // Reflejar al instante en la lista de la pantalla
        _uiState.update { current ->
            current.copy(
                contacts = listOf(tempContact) + current.contacts,
                saving = true,
                errorMessage = null,
                successMessage = null
            )
        }

        // Ejecutar callback inmediato para limpiar inputs y ocultar teclado
        onImmediateSuccess()

        // 2. Persistir en segundo plano en Supabase
        viewModelScope.launch {
            try {
                val insertData = ContactInsert(
                    firstName = cleanFirstName,
                    lastName = cleanLastName,
                    username = cleanUsername,
                    countryCode = cleanCountryCode,
                    phone = cleanPhone,
                    email = cleanEmail,
                    syncToPhone = syncToPhone
                )

                val created = withContext(Dispatchers.IO) {
                    repository.create(insertData)
                }

                // Reemplazar el provisional por el definitivo
                _uiState.update { current ->
                    current.copy(
                        contacts = current.contacts.map { if (it.id == tempId) created else it },
                        saving = false,
                        successMessage = "Contacto \"${created.fullName}\" guardado exitosamente"
                    )
                }
            } catch (e: Exception) {
                // Si la BD rechaza el guardado, revertir el temporal
                _uiState.update { current ->
                    current.copy(
                        contacts = current.contacts.filterNot { it.id == tempId },
                        saving = false
                    )
                }

                val rawMsg = e.message.orEmpty()
                val friendlyMessage = when {
                    rawMsg.contains("unique_contact_phone", ignoreCase = true) ->
                        "Este número de teléfono ya está registrado con el código $cleanCountryCode."
                    rawMsg.contains("unique_contact_fullname", ignoreCase = true) ->
                        "Ya existe un contacto con el nombre \"$cleanFirstName $cleanLastName\". Por favor, utilice otro nombre."
                    rawMsg.contains("username", ignoreCase = true) || rawMsg.contains("contacts_username_key", ignoreCase = true) ->
                        "El nombre de usuario \"@$cleanUsername\" ya está en uso."
                    rawMsg.contains("check", ignoreCase = true) ->
                        "Los datos no cumplen con las reglas requeridas por la aplicación."
                    else ->
                        "Error al guardar el contacto: ${e.localizedMessage ?: "Intente nuevamente"}"
                }

                _uiState.update { it.copy(errorMessage = friendlyMessage) }
                onError(friendlyMessage)
            }
        }
    }

    /**
     * Elimina un contacto de Supabase por su ID único.
     */
    fun deleteContact(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // Eliminacion optimista inmediata de la pantalla
            val previousList = _uiState.value.contacts
            _uiState.update { current ->
                current.copy(contacts = current.contacts.filterNot { it.id == id })
            }

            try {
                withContext(Dispatchers.IO) {
                    repository.delete(id)
                }
                _uiState.update { it.copy(successMessage = "Contacto eliminado exitosamente") }
                onSuccess()
            } catch (e: Exception) {
                // Si falla la eliminacion en la nube, restaurar en la lista
                _uiState.update { current ->
                    current.copy(
                        contacts = previousList,
                        errorMessage = "Error al eliminar el contacto: ${e.localizedMessage ?: "Intente de nuevo"}"
                    )
                }
            }
        }
    }

    /**
     * Limpia los mensajes de notificación / error.
     */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
