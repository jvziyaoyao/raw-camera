import camera.flow.compileSdk
import camera.flow.minSdk
import camera.flow.targetSdk

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin)
}

android {
    namespace = "com.jvziyaoyao.raw.camera"
    compileSdk = project.compileSdk

    defaultConfig {
        applicationId = "com.jvziyaoyao.raw.camera"
        minSdk = project.minSdk
        targetSdk = project.targetSdk
        versionCode = 1
        versionName = "1.0.1-alpha.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":camera-flow"))

//    implementation("com.github.jvziyaoyao:ImageViewer:feat-zoomable-SNAPSHOT")
//    implementation("com.jvziyaoyao.scale:image-viewer:1.1.0-alpha.4")
    implementation("com.jvziyaoyao.scale:image-viewer:1.1.0-test.5")
//    implementation("com.jvziyaoyao.scaler:image-viewer:1.1.0-test.5")
//    implementation("com.github.jvziyaoyao:ImageViewer:1.1.0-alpha.1")
//    implementation("com.github.jvziyaoyao:ImageViewer:1.0.2-alpha.8")

    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.coil.compose)

    implementation(libs.google.accompanist.permissions)
    implementation(libs.google.accompanist.systemuicontroller)

    implementation(libs.io.insert.koin.android)
    testImplementation(libs.io.insert.koin.test)
    testImplementation(libs.io.insert.koin.test.junit4)
    implementation(libs.io.insert.koin.androidx.compose)

    implementation(libs.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}