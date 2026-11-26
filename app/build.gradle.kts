plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.metroplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.metroplayer"
        minSdk = 26
        targetSdk = 35
        // versionCode растёт с каждой сборкой в GitHub Actions — новая версия ставится поверх старой
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0) + 100
        versionName = "2.7"
    }

    // Один и тот же ключ для всех сборок: без него каждая сборка в CI подписывается новым случайным
    // debug-ключом, и Android не даёт обновить приложение, не удалив прежнее.
    signingConfigs {
        create("metro") {
            storeFile = file("metroplayer.keystore")
            storePassword = "metroplayer"
            keyAlias = "metroplayer"
            keyPassword = "metroplayer"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("metro")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("metro")
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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
}
