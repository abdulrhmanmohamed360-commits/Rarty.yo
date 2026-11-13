plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cuptracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cuptracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += "tflite" }
}

dependencies {
    implementation(project(":tracker-core"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.opencv:opencv:4.9.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
}
