plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// versionCode grows with every cloud build, so each push is an update on the phone.
val runNumber: Int = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toIntOrNull() ?: 1

android {
    namespace = "com.mughaltraders.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mughaltraders.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.0"
    }

    buildTypes {
        release {
            // The shop installs the debug-signed APK; this build type is kept for completeness.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Must live at top level (outside android {}) or AGP refuses the script.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// No third-party libraries: WebView + JDK networking only, so the cloud
// build stays fast and cannot break on an upstream dependency.
