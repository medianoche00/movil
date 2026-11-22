package com.unp.emprendepiura

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.unp.emprendepiura.core.notifications.EmprendeNotificationHelper
import com.unp.emprendepiura.core.theme.EmprendePiuraTheme
import com.unp.emprendepiura.ui.nav.AppNav

/*
 * Archivo: MainActivity.kt
 * Proposito: Punto de entrada de EmprendePiura. Hereda de FragmentActivity para dar soporte
 *            nativo al lector fisico de huella dactilar (BiometricPrompt) y configura canales
 *            de notificaciones nativas para mensajes y pedidos en tiempo real.
 */
class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permiso gestionado por el usuario; la app funciona normalmente si se rechaza */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        EmprendeNotificationHelper.crearCanales(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !EmprendeNotificationHelper.tienePermiso(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
