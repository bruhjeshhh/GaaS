import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Signing material never lives in git. Drop a keystore.properties at the repo
// root when you're ready to upload a bundle; CI keeps building unsigned.
val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}

// Google Sign-In needs the OAuth *Web* client id, not the Android one. An env
// var wins so CI can inject it without touching local.properties.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
val webClientId = (System.getenv("GOOGLE_WEB_CLIENT_ID")
    ?: localProps.getProperty("google.web.client.id", ""))
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.brajesh.gaas"
    // 36 is the floor for a Google Play submission as of 2026-08-31, and it
    // also flips on enforced edge-to-edge — see MainActivity for the insets.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.brajesh.gaas"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "2.0.0"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
    }

    signingConfigs {
        create("release") {
            val storePath = signingProps.getProperty("storeFile")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingProps.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        // Lets the app read its own VERSION_NAME for the update check.
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Room for local persistence — every logged meal and the day's goal live on-device
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Networking + JSON — talks straight to Gemini using the user's own key
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // EncryptedSharedPreferences for the API key + daily goal
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Google Sign-In through the platform Credential Manager (no Firebase needed)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    // GetGoogleIdOption / GoogleIdTokenCredential ship in this separate artifact.
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
