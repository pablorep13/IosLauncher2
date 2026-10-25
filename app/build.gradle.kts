plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.launchertest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.launchertest"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":liquidglass"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
}
