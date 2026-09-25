plugins {
    alias(libs.plugins.android.application)
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

android {
    namespace = "com.example.std"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.std"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Supabase (BOM управляет версиями)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    // Ktor
    implementation("io.ktor:ktor-client-android:3.0.3")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
