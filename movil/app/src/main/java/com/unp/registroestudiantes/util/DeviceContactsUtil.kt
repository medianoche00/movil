package com.unp.registroestudiantes.util

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract

/*
 * Archivo: DeviceContactsUtil.kt
 * Proposito: Utilidad para insertar contactos directamente en la agenda nativa del dispositivo Android.
 *
 * Utiliza el ContentResolver y ContactsContract para crear el contacto en segundo plano.
 */
object DeviceContactsUtil {

    /**
     * Guarda un nuevo contacto en el almacenamiento local del telefono utilizando ContactsContract.
     * Retorna true si la operacion fue exitosa, o false si ocurrio una excepcion.
     */
    fun saveContactToDevice(
        context: Context,
        firstName: String,
        lastName: String,
        phone: String,
        email: String?
    ): Boolean {
        return try {
            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            // 1. Crear RawContact inicial
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Asignar Nombre y Apellido
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                    .build()
            )

            // 3. Asignar Numero de Telefono Celular
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            // 4. Asignar Correo Electronico si fue provisto
            if (!email.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        )
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.trim())
                        .withValue(
                            ContactsContract.CommonDataKinds.Email.TYPE,
                            ContactsContract.CommonDataKinds.Email.TYPE_HOME
                        )
                        .build()
                )
            }

            // Aplicar todas las operaciones atómicamente en el ContentResolver
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
