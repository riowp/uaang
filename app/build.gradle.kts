plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rio.keuanganku"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rio.keuanganku"
        minSdk = 29
        targetSdk = 34
        // versionCode otomatis naik setiap build di GitHub Actions,
        // sehingga APK baru selalu bisa menimpa (update) versi yang terinstal.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.plus(100) ?: 100
        versionName = "1.7"
    }

    // Keystore permanen: semua build ditandatangani kunci yang sama,
    // jadi update APK bisa langsung install di atas versi lama tanpa uninstall.
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("keystore/keuanganku.jks")
            storePassword = "keuanganku123"
            keyAlias = "keuanganku"
            keyPassword = "keuanganku123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // OCR scan struk — on-device, tetap offline setelah install
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Passcode + biometric fingerprint
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}
