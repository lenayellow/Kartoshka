import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

// Signing config resolution priority:
//   1. Env vars KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD  (CI)
//   2. keystore.properties in project root  (local dev)
//   3. Nothing — debug builds continue normally; release tasks fail with a clear message
val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}

val ksStorePath: String? =
    System.getenv("KEYSTORE_PATH")
        ?: keystoreProps.getProperty("storeFile")?.takeIf { it.isNotEmpty() }
val ksStorePassword: String? =
    System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
val ksKeyAlias: String? =
    System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
val ksKeyPassword: String? =
    System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")

// ── Versioning ────────────────────────────────────────────────────────────────
// versionCode  : git rev-list --count HEAD  →  1 (fallback)
// versionName  : nearest git tag vX.Y.Z → X.Y.Z  →  VERSION file  →  "0.1.0-dev"
// Tag releases as "v1.0.0", "v1.2.3", etc.

fun gitExec(vararg args: String): String = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
} catch (_: Exception) { "" }

val versionFallback: String =
    rootProject.file("VERSION").takeIf { it.exists() }?.readText()?.trim() ?: "0.1.0-dev"

val gitAvailable: Boolean = gitExec("rev-parse", "--git-dir").isNotEmpty()

val computedVersionCode: Int =
    if (gitAvailable) gitExec("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
    else { logger.warn("[version] git not available — using versionCode=1"); 1 }

val computedVersionName: String =
    if (gitAvailable)
        gitExec("describe", "--tags", "--abbrev=0", "--match", "v*")
            .removePrefix("v")
            .ifEmpty { versionFallback }
    else versionFallback

logger.lifecycle("[version] versionCode=$computedVersionCode  versionName=$computedVersionName")

android {
    namespace = "com.lena.kartoshka"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lena.kartoshka"
        minSdk = 26
        targetSdk = 34
        versionCode = computedVersionCode
        versionName = computedVersionName
        buildConfigField(
            "String", "APPMETRICA_API_KEY",
            "\"${localProps.getProperty("APPMETRICA_API_KEY", "")}\""
        )
        // Release URL — replace CLOUD_FUNCTION_PLACEHOLDER before deploying to production (Sprint 5)
        buildConfigField(
            "String", "API_BASE_URL",
            "\"https://CLOUD_FUNCTION_PLACEHOLDER.apigw.yandexcloud.net/\""
        )
    }

    signingConfigs {
        create("release") {
            if (ksStorePath != null) {
                storeFile = rootProject.file(ksStorePath)
                storePassword = ksStorePassword
                keyAlias = ksKeyAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // localhost works via adb reverse tunnel (USB); for emulator use 10.0.2.2 instead
            buildConfigField("String", "API_BASE_URL", "\"http://localhost:8080/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ksStorePath != null) {
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
        buildConfig = true
    }
}

// Fail fast with a human-readable message if release is built without signing config
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(ksStorePath != null) {
            "Release signing not configured.\n" +
            "Set env vars KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD,\n" +
            "or create keystore.properties in the project root (see keystore.properties.example)."
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Drag-and-drop reordering
    implementation(libs.reorderable)

    // AppMetrica — crash reporting and analytics
    implementation(libs.appmetrica.analytics)

    // ZXing — barcode rendering
    implementation("com.google.zxing:core:3.5.3")

    // ML Kit — barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.security.crypto)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
