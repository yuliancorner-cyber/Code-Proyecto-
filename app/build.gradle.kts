// Archivo de compilacion del modulo "app" (la aplicacion en si).

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    // "namespace" = el paquete Kotlin/Java base del codigo.
    namespace = "com.focuszone.app"

    // compileSdk = version de Android contra la que COMPILAMOS (APIs disponibles).
    compileSdk = 35

    defaultConfig {
        // applicationId = la "matricula" unica de la app en el celular.
        // Dos apps con el mismo applicationId no pueden convivir.
        applicationId = "com.focuszone.app"

        // minSdk = version MINIMA de Android donde la app se puede instalar.
        // 26 = Android 8.0 (Oreo).
        minSdk = 26

        // targetSdk = version de Android para la que la app esta "afinada".
        targetSdk = 35

        // versionCode: numero interno que DEBE subir en cada APK que instales
        // encima de otro. versionName: el que ves en Ajustes > Apps.
        versionCode = 10
        versionName = "1.0"

        // Nombre visible de la app (en el menu y en Accesibilidad). La version
        // de desarrollo lo cambia, ver buildTypes > debug.
        manifestPlaceholders["nombreApp"] = "FocusZone"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // "release" = la version final que instalas para usar a diario.
        release {
            // R8 = optimizador: quita codigo y recursos que no se usan y
            // reduce el tamano del APK. Las reglas de que conservar estan en
            // proguard-rules.pro (Room y Compose traen las suyas).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // "debug" = la version que instala Android Studio con Run.
        // Es una app APARTE ("FocusZone Dev", com.focuszone.app.debug) para que
        // conviva con la version final firmada: puedes probar cambios sin
        // desinstalar la app de uso diario ni perder sus datos.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            manifestPlaceholders["nombreApp"] = "FocusZone Dev"
        }
    }

    lint {
        // Al generar el APK de release, Android Studio pasa una revision
        // automatica ("lint"). Si encuentra algo grave lo muestra, pero no
        // bloquea el APK: es una app personal y prefiero que puedas generarlo.
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        // Activa Jetpack Compose (construir la interfaz con codigo Kotlin, no con XML).
        compose = true
    }
}

dependencies {
    // El BOM fija versiones compatibles de todo Compose.
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Base de datos Room. "ksp(...)" = generador que escribe el codigo de la BD por nosotros.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Camara (CameraX)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Herramientas de vista previa: solo en la version de desarrollo.
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
