// The Python runtime (Chaquopy) and the script that runs a student's code. Only :pyfeature (an
// on-demand module) depends on this, so none of it ships in the base app.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "com.prostuti.pyruntime"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        // 32-bit ARM too: Python 3.11 is the newest Chaquopy build for armeabi-v7a (budget phones).
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
    }
}

dependencies {
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
