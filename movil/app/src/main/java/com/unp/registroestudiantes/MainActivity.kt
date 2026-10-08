package com.unp.registroestudiantes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.unp.registroestudiantes.ui.nav.AppNav
import com.unp.registroestudiantes.ui.theme.AppTheme

/*
 * Archivo: MainActivity.kt
 * Proposito: Punto de entrada principal de la aplicacion Android.
 *
 * Guia de transicion Java -> Kotlin / Jetpack Compose:
 *
 * 1. Arquitectura "Single-Activity":
 *    En el desarrollo clasico de Android (Java con XML), cada pantalla solia
 *    ser una Activity separada (ListActivity, FormActivity, DetailActivity).
 *    En el enfoque moderno con Jetpack Compose se utiliza una unica Activity
 *    (MainActivity) y la navegacion entre pantallas se gestiona mediante
 *    funciones @Composable usando NavHost (similar a una Single Page Application
 *    o SPA en JavaScript con React Router o Vue Router).
 *
 * 2. ComponentActivity vs AppCompatActivity:
 *    ComponentActivity es la clase base ligera necesaria para Jetpack Compose.
 *    No arrastra los temas antiguos de ActionBar ni dependencias obsoletas de XML.
 *
 * 3. setContent { ... } en lugar de setContentView(R.layout.activity_main):
 *    En Java clasico, se inflaba un archivo XML.
 *    En Compose, la interfaz se declara directamente en codigo Kotlin
 *    mediante funciones con la anotacion @Composable.
 *
 * 4. enableEdgeToEdge():
 *    Permite que la app dibuje su contenido detras de la barra de estado superior
 *    (reloj y bateria) y de la barra de navegacion inferior (gestos), logrando
 *    una apariencia visual moderna inmersiva.
 *
 * 5. AppTheme y Surface:
 *    - AppTheme: Envuelve la interfaz aplicando la paleta de colores, tipografia
 *      y formas globales (equivalente al archivo styles.xml o a una hoja de estilos
 *      CSS raiz :root).
 *    - Surface: Contenedor visual base con el color de fondo del tema
 *      (equivalente a la etiqueta <body> en HTML).
 *    - AppNav(): Funcion que orquesta las pantallas y rutas de la aplicacion.
 */
class MainActivity : ComponentActivity() {

    // En Java: @Override protected void onCreate(Bundle savedInstanceState)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita diseno de borde a borde en la pantalla
        enableEdgeToEdge()

        // Define el arbol de componentes graficos (Compose)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }
}
