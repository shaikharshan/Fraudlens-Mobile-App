import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.fraudlens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fraudlens"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        fun localSecret(name: String): String {
            return localProperties.getProperty(name) ?: ""
        }

        buildConfigField("String", "GEMINI_API_KEY", "\"${localSecret("GEMINI_API_KEY")}\"")
        buildConfigField("String", "IPDB_API_KEY", "\"${localSecret("IPDB_API_KEY")}\"")
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"${localSecret("RAZORPAY_KEY_ID")}\"")
        buildConfigField("String", "RAZORPAY_KEY_SECRET", "\"${localSecret("RAZORPAY_KEY_SECRET")}\"")


    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}


dependencies {
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.appcompat)


    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.5.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Biometric (stable release)
    implementation("androidx.biometric:biometric:1.1.0")

    //hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Location & Permissions
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.accompanist:accompanist-permissions:0.33.0-alpha")



    // WebSocket support
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Audio recording
    implementation("androidx.media3:media3-common:1.2.0")


// ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.camera.mlkit.vision)

// CameraX dependencies for camera integration
    implementation(libs.androidx.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    //razorpay
    implementation("com.razorpay:checkout:1.6.40")

    // Compose and UI libraries
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}