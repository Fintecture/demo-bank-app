plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fintecture.demobank"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fintecture.demobank"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Signing material never lives in this repository. Point FTE_DEMOBANK_KEYSTORE at the keystore
    // and FTE_DEMOBANK_KEYSTORE_PASSWORD at its password to produce a signed release build;
    // without them the release build is simply unsigned rather than failing.
    val keystorePath: String? = System.getenv("FTE_DEMOBANK_KEYSTORE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("FTE_DEMOBANK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FTE_DEMOBANK_KEY_ALIAS") ?: "demobank"
                keyPassword = System.getenv("FTE_DEMOBANK_KEY_PASSWORD")
                    ?: System.getenv("FTE_DEMOBANK_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        // Stranding reproduces a Connect bug we have not fixed yet, so a build handed to an
        // integrator must not offer it. Debug keeps it for our own app2app work.
        debug {
            buildConfigField("boolean", "SHOW_DIAGNOSTIC_ACTIONS", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "SHOW_DIAGNOSTIC_ACTIONS", "false")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
