// Archivo de compilacion del modulo "app" (la aplicacion en si).

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

        versionCode = 1
        versionName = "0.1-fase0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // "release" = version final optimizada. "debug" = version de desarrollo.
        release {
            // Por ahora sin ofuscacion, para que los errores sean legibles.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Herramientas de vista previa: solo en la version de desarrollo.
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
