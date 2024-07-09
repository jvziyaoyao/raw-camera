import camera.flow.compileSdk
import camera.flow.minSdk

plugins {
//    id("com.android.library")
//    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin)
}

val GROUP_NAME = "com.github.jvziyaoyao"
val ARTIFACT_NAME = "camera-raw"
val VERSION_CODE = 1
val VERSION_NAME = "1.0.1-alpha.1"

group = GROUP_NAME
version = VERSION_NAME

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = GROUP_NAME
                artifactId = ARTIFACT_NAME
                version = VERSION_NAME
            }
        }
        repositories {
            // 发布的时候需要去除
            mavenLocal()
        }
    }
}

android {
    namespace = "com.jvziyaoyao.camera.flow"
    compileSdk = project.compileSdk

    defaultConfig {
        minSdk = project.minSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(libs.org.opencv.opencv)
    api(libs.androidx.exif)
//    api("org.opencv:opencv:4.9.0")
//    api("androidx.exifinterface:exifinterface:1.3.7")

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

//    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
//    implementation("androidx.compose.ui:ui")
//    implementation("androidx.compose.ui:ui-graphics")

    implementation(libs.core.ktx)
    implementation(libs.appcompat)

//    implementation("androidx.core:core-ktx:1.12.0")
//    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("com.google.android.material:material:1.11.0")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.5")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}