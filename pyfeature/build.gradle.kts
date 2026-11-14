// On-demand module: Play downloads it the first time a student runs Python code. It only carries
// :pyruntime (Chaquopy + the runner script); the base app calls into it by reflection.
plugins {
    alias(libs.plugins.android.dynamic.feature)
}

android {
    namespace = "com.prostuti.student.pyfeature"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":pyruntime"))
}
