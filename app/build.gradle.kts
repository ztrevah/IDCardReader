plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.id_card_reader"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.id_card_reader"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.text.recognition)
    implementation("com.google.guava:guava:27.0.1-android")
    implementation("net.sf.scuba:scuba-sc-android:0.0.18")
    implementation("org.jmrtd:jmrtd:0.7.18")
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("org.tensorflow:tensorflow-lite:2.4.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.0.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
    implementation("androidx.activity:activity:1.9.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}