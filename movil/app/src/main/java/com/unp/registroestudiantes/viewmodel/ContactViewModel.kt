package com.unp.registroestudiantes.viewmodel

import androidx.lifecycle.ViewModel
import com.unp.registroestudiantes.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel que gestiona la lista de contactos en memoria.
 * Permite agregar nuevos contactos con (nombre, celular, correo) y eliminarlos.
 */
class ContactViewModel : ViewModel() {

    // Lista inicial de contactos de prueba
    private val _contacts = MutableStateFlow<List<Contact>>(
        listOf(
            Contact(
                name = "Juan Pérez",
                phone = "987654321",
                email = "juan.perez@unp.edu.pe"
            ),
            Contact(
                name = "María López",
                phone = "912345678",
                email = "maria.lopez@gmail.com"
            ),
            Contact(
                name = "Carlos Mendoza",
                phone = "955443322",
                email = "carlos.mendoza@outlook.com"
            )
        )
    )
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    /**
     * Agrega un nuevo contacto al inicio de la lista.
     */
    fun addContact(name: String, phone: String, email: String) {
        val newContact = Contact(
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim()
        )
        _contacts.update { current -> listOf(newContact) + current }
    }

    /**
     * Elimina un contacto de la lista por su identificador único (id).
     */
    fun deleteContact(id: String) {
        _contacts.update { current -> current.filterNot { it.id == id } }
    }
}
