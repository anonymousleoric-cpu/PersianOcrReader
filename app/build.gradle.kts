plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sahandsoft.matnkhan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sahandsoft.matnkhan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release build is signed with the debug key so it can be installed
    // directly for local/personal distribution. Replace with a real
    // signing config before publishing to Google Play.
    signingConfigs {
        getByName("debug") {
            // default debug keystore
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Jetpack Compose (BOM-managed)
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Settings persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Image preview
    implementation("io.coil-kt:coil-compose:2.7.0")

    // EXIF rotation handling
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // OCR engine: Tesseract 5 with LSTM, shipped as native libs (offline).
    // Persian (fas) + English (eng) trained models are bundled in assets.
    implementation("com.github.adaptech-cz.Tesseract4Android:tesseract4android-openmp:4.8.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
}
