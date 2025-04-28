plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.tiococo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tiococo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity)
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    // Dependencias básicas
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // UI tradicional
    implementation(libs.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)

    implementation(libs.androidx.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Serialización
    implementation(libs.kotlinx.serialization.json)

    // Corrutinas (si no la tienes ya)
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager con soporte para corrutinas (Kotlin)
    implementation(libs.androidx.work.runtime.ktx)

    implementation("com.google.firebase:firebase-firestore-ktx:25.1.3")
    implementation("com.google.firebase:firebase-auth-ktx:23.2.0")
    implementation("com.google.firebase:firebase-common-ktx:21.0.0")
    implementation ("org.mindrot:jbcrypt:0.4")
    implementation ("org.jsoup:jsoup:1.15.3")
}