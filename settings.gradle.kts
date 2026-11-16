// Este archivo le dice a Gradle (el sistema de compilacion de Android)
// DE DONDE bajar las librerias y QUE modulos forman el proyecto.

pluginManagement {
    repositories {
        // Repositorio de Google: contiene el plugin de Android y las librerias AndroidX.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Prohibe declarar repositorios sueltos en cada modulo: todos se declaran aqui.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FocusZone"

// El proyecto tiene dos modulos, y cada uno es una app independiente:
//  - "app"    = FocusZone (enfoque para estudiar).
//  - "jarvis" = JARVIS (asistente personal: tareas, habitos, alarma con voz, notas).
// En Android Studio eliges cual instalar en el desplegable junto al boton Run.
include(":app")
include(":jarvis")
