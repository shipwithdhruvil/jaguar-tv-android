plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.screenlite.webkiosk"
    compileSdk = 37

    defaultConfig {
        // The Kotlin package and namespace stay org.screenlite.webkiosk so upstream merges stay clean.
        applicationId = "in.jaguarfitnessclub.tv"
        minSdk = 23
        targetSdk = 36
        versionCode = 11
        versionName = "0.0.11"
        buildConfigField("String", "LIVE_URL", "\"https://desk.jaguarfitnessclub.in/tv\"")
    }

    val releaseKeystore = file("release.keystore")

    signingConfigs {
        if (releaseKeystore.exists()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        // Plain HTTP only in debug builds, to reach a dev server; the network security
        // config (src/debug) does the same on Android 7 and later.
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            // Debug builds can point at this Mac or any address; release is always live.
            buildConfigField("boolean", "SERVER_CHOICE", "true")
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("boolean", "SERVER_CHOICE", "false")
            // Without the keystore (local builds) the release APK is left unsigned.
            if (releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.datastore.preferences)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}