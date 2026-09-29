package com.unp.registroestudiantes.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unp.registroestudiantes.ui.screens.DetailScreen
import com.unp.registroestudiantes.ui.screens.FormScreen
import com.unp.registroestudiantes.ui.screens.ListScreen
import com.unp.registroestudiantes.viewmodel.StudentViewModel

private const val LIST = "list"
private const val FORM_NEW = "form"
private const val FORM_EDIT = "form/{id}"
private const val DETAIL = "detail/{id}"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNav() {
    val nav: NavHostController = rememberNavController()
    val vm: StudentViewModel = viewModel()
    val state by vm.state.collectAsState()

    // Muestra errores de guardar/eliminar como snackbar simple (Text superior temporal)
    state.actionError?.let { msg ->
        LaunchedEffect(msg) { /* el propio Scaffold de cada pantalla puede mostrarlo si se desea */ }
    }

    SharedTransitionLayout {
        NavHost(navController = nav, startDestination = LIST) {
            composable(LIST) {
                ListScreen(
                    students = state.students,
                    loading = state.loading,
                    loadError = state.loadError,
                    onRetry = { vm.load() },
                    onAdd = { nav.navigate(FORM_NEW) },
                    onOpen = { s -> nav.navigate("detail/${s.id}") }
                )
            }
            composable(FORM_NEW) {
                FormScreen(
                    editing = null, saving = state.saving,
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
            composable(FORM_EDIT) { entry ->
                val id = entry.arguments?.getString("id")
                val editing = state.students.firstOrNull { it.id == id }
                FormScreen(
                    editing = editing, saving = state.saving,
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
            composable(DETAIL) { entry ->
                val id = entry.arguments?.getString("id")
                val student = state.students.firstOrNull { it.id == id }
                if (student == null) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                } else {
                    DetailScreen(
                        student = student,
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate("form/${student.id}") },
                        onDelete = { vm.delete(student.id) { nav.popBackStack(LIST, false) } }
                    )
                }
            }
        }
    }
}
