import java.time.Duration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.prostuti.student"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.prostuti.student"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://testbe.prostuti.com.bd\"")
            // Crashlytics off in debug: crashes and errors stay in Logcat only.
            manifestPlaceholders["crashlyticsEnabled"] = false
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://testbe.prostuti.com.bd\"")
            manifestPlaceholders["crashlyticsEnabled"] = true
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// A ViewModel with an infinite ticker can spin runTest's virtual clock forever; fail instead of hanging.
tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(5))
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.core)
    // Navigation3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    // Ktor Network
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    // Koin DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    // Images
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    // Google font
    implementation(libs.androidx.compose.ui.text.google.fonts)
    // Logging
    implementation(libs.timber)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // Database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    // Push notifications (FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.kotlinx.coroutines.play.services)
    // LaTeX renderer
    implementation(libs.latex.base)
    implementation(libs.latex.parser)
    implementation(libs.latex.renderer)
    // Home screen widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.ktor.bom))
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.androidx.glance.appwidget.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.ktor.bom))
    androidTestImplementation(libs.ktor.client.mock)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}