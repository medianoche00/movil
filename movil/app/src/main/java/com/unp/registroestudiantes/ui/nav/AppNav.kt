package com.unp.registroestudiantes.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unp.registroestudiantes.ui.screens.ContactsScreen
import com.unp.registroestudiantes.ui.screens.DetailScreen
import com.unp.registroestudiantes.ui.screens.FormScreen
import com.unp.registroestudiantes.ui.screens.ListScreen
import com.unp.registroestudiantes.ui.screens.MenuScreen
import com.unp.registroestudiantes.ui.screens.SplashScreen
import com.unp.registroestudiantes.viewmodel.ContactViewModel
import com.unp.registroestudiantes.viewmodel.StudentViewModel

/*
 * Archivo: AppNav.kt
 * Proposito: Enrutador y gestor de navegacion de la aplicacion (Navigation Graph).
 *
 * Guia de transicion Desarrollo Web (React Router) -> Jetpack Compose Navigation:
 *
 * 1. Rutas basadas en cadenas de texto (Strings):
 *    Al igual que en React Router o Express.js:
 *      - "splash": Pantalla de bienvenida con animacion.
 *      - "list": Pantalla con la lista y cuadricula de estudiantes.
 *      - "form": Formulario para crear un nuevo estudiante.
 *      - "form/{id}": Formulario para editar un estudiante existente por parametro.
 *      - "detail/{id}": Pantalla de detalle y resena del estudiante.
 *
 * 2. rememberNavController():
 *    Crea y recuerda el controlador del historial de navegacion (pila de pantallas).
 *    Equivale al objeto 'history' o 'useNavigate()' en React Router.
 *
 * 3. viewModel():
 *    Obtiene la instancia de StudentViewModel vinculada al ciclo de vida de la Activity.
 *    No se destruye al navegar entre pantallas; todos los destinos comparten el mismo estado.
 *
 * 4. collectAsState():
 *    Convierte el flujo reactivo StateFlow del ViewModel en un State de Compose.
 *    Cada vez que el ViewModel emite un nuevo UiState, Compose recompone unicamente
 *    las partes de la pantalla que leen ese valor (similar al hook useSelector en Redux).
 *
 * 5. popUpTo("splash") { inclusive = true }:
 *    Al terminar el SplashScreen, se navega a "list" y se remueve "splash" del historial.
 *    Esto evita que si el usuario presiona el boton fisico "Atras" en el celular,
 *    regrese a la pantalla de bienvenida.
 */

// Constantes de rutas de navegacion
private const val SPLASH    = "splash"
private const val MENU      = "menu"
private const val LIST      = "list"
private const val FORM_NEW  = "form"
private const val FORM_EDIT = "form/{id}"
private const val DETAIL    = "detail/{id}"
private const val CONTACTS  = "contacts"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNav() {
    // Controlador de la pila de navegacion
    val nav: NavHostController = rememberNavController()

    // Instancia unica de los ViewModels compartida por las pantallas
    val vm: StudentViewModel = viewModel()
    val contactVm: ContactViewModel = viewModel()

    // Observa el estado reactivo del ViewModel
    val state by vm.state.collectAsState()
    val contactsState by contactVm.contacts.collectAsState()

    SharedTransitionLayout {
        NavHost(navController = nav, startDestination = SPLASH) {

            // Destino 1: Pantalla de inicio animada (Splash)
            composable(SPLASH) {
                SplashScreen(
                    onFinished = {
                        // Navega al panel principal y remueve el Splash del historial
                        nav.navigate(MENU) {
                            popUpTo(SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // Destino 2: Panel Principal con selector de los 2 modulos
            composable(MENU) {
                MenuScreen(
                    studentsCount = state.students.size,
                    contactsCount = contactsState.size,
                    onOpenStudents = { nav.navigate(LIST) },
                    onOpenContacts = { nav.navigate(CONTACTS) }
                )
            }

            // Destino 3: Listado de estudiantes (Lista o Cuadricula)
            composable(LIST) {
                ListScreen(
                    students = state.students,
                    loading = state.loading,
                    loadError = state.loadError,
                    onRetry = { vm.load() },
                    onAdd = { nav.navigate(FORM_NEW) },
                    onOpen = { s -> nav.navigate("detail/${s.id}") },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(CONTACTS) {
                ContactsScreen(
                    contacts = contactsState,
                    onBack = { nav.popBackStack() },
                    onAddContact = { name, phone, email -> contactVm.addContact(name, phone, email) },
                    onDeleteContact = { id -> contactVm.deleteContact(id) }
                )
            }

            // Destino 3: Formulario para registrar un nuevo estudiante
            composable(FORM_NEW) {
                FormScreen(
                    editing = null,
                    saving = state.saving,
                    errorMessage = state.actionError,
                    onClearError = { vm.clearError() },
                    onCancel = { nav.popBackStack() },
                    onSave = { input ->
                        vm.save(null, input) { newId ->
                            nav.navigate("detail/$newId") { popUpTo(LIST) }
                        }
                    }
                )
            }

            // Destino 4: Formulario para editar un estudiante existente
            composable(FORM_EDIT) { entry ->
                val id = entry.arguments?.getString("id")
                val editing = state.students.firstOrNull { it.id == id }
                FormScreen(
                    editing = editing,
                    saving = state.saving,
                    errorMessage = state.actionError,
                    onClearError = { vm.clearError() },
                    onCancel = { nav.popBackStack() },
                    onSave = { input ->
                        vm.save(id, input) { savedId ->
                            nav.navigate("detail/$savedId") { popUpTo(LIST) }
                        }
                    }
                )
            }

            // Destino 5: Detalle completo del estudiante y resena generada
            composable(DETAIL) { entry ->
                val id = entry.arguments?.getString("id")
                val student = state.students.firstOrNull { it.id == id }

                if (student == null) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    DetailScreen(
                        student = student,
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate("form/${student.id}") },
                        onDelete = {
                            vm.delete(student.id) {
                                nav.popBackStack(LIST, false)
                            }
                        }
                    )
                }
            }
        }
    }
}
