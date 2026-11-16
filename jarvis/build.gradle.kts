// Archivo de compilacion del modulo "jarvis": la app JARVIS (asistente personal).
// Es una app INDEPENDIENTE de FocusZone: otro identificador, otros datos.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jarvis.app"
    compileSdk = 35

    defaultConfig {
        // "Matricula" de la app en el celular. Distinta de la de FocusZone,
        // asi que las dos conviven sin problema.
        applicationId = "com.jarvis.app"
        minSdk = 26
        targetSdk = 35

        // versionCode DEBE subir en cada APK que instales encima de otro.
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["appName"] = "JARVIS"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Version final firmada, para usar a diario (igual que en FocusZone).
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Version de pruebas que instala Android Studio con Run: una app aparte
        // ("JARVIS Dev") para no pisar los datos de la version final.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appName"] = "JARVIS Dev"
        }
    }

    lint {
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
        compose = true
    }

    testOptions {
        unitTests {
            // Las pruebas con Robolectric necesitan los recursos (iconos, textos).
            isIncludeAndroidResources = true
            all {
                // Por defecto las pruebas de pantallas solo comprueban que todo
                // se dibuja. Con  -Pscreenshots  ademas guardan las imagenes en
                // jarvis/build/screenshots (ver README).
                it.systemProperty("roborazzi.test.record", providers.gradleProperty("screenshots").isPresent)
                it.maxHeapSize = "2g"
            }
        }
    }
}

dependencies {
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

    // Base de datos Room: tareas, habitos, rutina y notas.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    // org.json viene dentro de Android, pero las pruebas corren en el PC.
    testImplementation(libs.org.json)
    // Pruebas que necesitan "un Android": base de datos, alarmas y pantallas.
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
