package com.unp.emprendepiura

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
import com.unp.emprendepiura.core.theme.EmprendePiuraTheme
import com.unp.emprendepiura.ui.nav.AppNav

/*
 * Archivo: MainActivity.kt
 * Proposito: Punto de entrada de EmprendePiura. Hereda de FragmentActivity para dar soporte
 *            nativo al lector fisico de huella dactilar (BiometricPrompt).
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var darkTheme by rememberSaveable { mutableStateOf(false) }

            EmprendePiuraTheme(darkTheme = darkTheme) {
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
