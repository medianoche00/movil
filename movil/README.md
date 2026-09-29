# Registro de Estudiantes UNP — Frontend Android (Kotlin + Compose + Supabase)

## Pasos
1. Android Studio > New Project > **Empty Activity** (Compose). Package: `com.unp.registroestudiantes`. Min SDK 26.
2. Copia la carpeta `app/src/main/java/...` de este zip sobre la de tu proyecto (reemplaza MainActivity.kt y borra el tema generado por defecto si choca).
3. Aplica lo de `gradle-snippets.txt` y agrega en `AndroidManifest.xml`, dentro de `<manifest>`:
   `<uses-permission android:name="android.permission.INTERNET" />`
4. Pon `SUPABASE_URL` y `SUPABASE_KEY` en `local.properties` (Supabase > botón Connect).
5. Corre `supabase_setup.sql` en el SQL Editor (políticas + buckets `photos` y `cvs`).
6. Sync Gradle y Run.

## Estructura
- data/: cliente Supabase, modelos, repositorio (CRUD + Storage)
- util/: extracción de texto del CV (PDFBox / ML Kit) y análisis por palabras clave
- viewmodel/: estado en StateFlow, orquesta guardado
- ui/: tema, animaciones (press, shimmer, stagger, pop), pantallas y navegación con shared element
