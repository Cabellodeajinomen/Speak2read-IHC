# Speak2Read

Aplicación móvil Android orientada a facilitar la comunicación accesible mediante texto y voz. Speak2Read permite mantener conversaciones, transcribir mensajes dictados, leer textos en voz alta y guardar mensajes importantes para consultarlos posteriormente. También incorpora autenticación de usuarios y detección de sonidos de emergencia para ofrecer una experiencia de comunicación más completa.

## Tecnologías utilizadas

- **Kotlin** como lenguaje principal.
- **Android SDK** y **AndroidX** para el desarrollo de la aplicación.
- **Layouts XML**, `RecyclerView` y **Material Design** para la interfaz.
- **Firebase Authentication** para el registro e inicio de sesión, incluido el acceso con Google.
- **Firebase Analytics** para el análisis de uso de la aplicación.
- **Room**, sobre **SQLite**, para almacenar localmente conversaciones, mensajes y favoritos.
- **SpeechRecognizer** para convertir voz en texto.
- **TextToSpeech** para convertir texto en voz.
- **TensorFlow Lite Task Audio** para apoyar la detección de sonidos.
- **Gradle Kotlin DSL** y **KSP** para la configuración y generación de código.

## Estructura del proyecto

```text
Speak2read-IHC/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/speak2read/
│   │   │   │   ├── data/       # Modelos y persistencia local con Room
│   │   │   │   ├── service/    # Servicios de la aplicación
│   │   │   │   └── ui/         # Pantallas, adaptadores y autenticación
│   │   │   ├── res/            # Layouts, textos, imágenes y estilos
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/        # Pruebas instrumentadas
│   │   └── test/               # Pruebas unitarias
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml      # Versiones y dependencias
├── build.gradle.kts
└── settings.gradle.kts
```

## Capturas de pantalla

> Esta sección se completará posteriormente con las capturas de las pantallas principales de la aplicación.

## Autor

**Alex Abdon Rhoddo Pacheco**
