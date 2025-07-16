plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.yourname.sanitizr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourname.sanitizr"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    implementation("com.mrljdx:ffmpeg-kit-full:6.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("org.tukaani:xz:1.9")
}

