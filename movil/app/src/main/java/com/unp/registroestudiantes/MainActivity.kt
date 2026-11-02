package com.unp.registroestudiantes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.unp.registroestudiantes.ui.nav.AppNav
import com.unp.registroestudiantes.ui.theme.AppTheme

/*
 * Archivo: MainActivity.kt
 * Proposito: Punto de entrada principal de la aplicacion Android.
 *            Hereda de FragmentActivity para dar soporte nativo al lector de huella (BiometricPrompt)
 *            y gestiona el estado global del Tema (Claro / Oscuro).
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(false) }

            AppTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(
                        darkTheme = darkTheme,
                        onToggleTheme = { darkTheme = !darkTheme }
                    )
                }
            }
        }
    }
}
