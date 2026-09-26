// Archivo de compilacion RAIZ del proyecto.
// Aqui solo declaramos que plugins existen; se aplican en cada modulo (ver app/build.gradle.kts).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
