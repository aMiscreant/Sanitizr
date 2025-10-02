plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.yourname.sanitizr"
    //noinspection GradleDependency
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourname.sanitizr"
        minSdk = 26
        //noinspection OldTargetApi
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
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    //noinspection GradleDependency
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    //noinspection Aligned16KB
    implementation("com.mrljdx:ffmpeg-kit-full:6.1.4")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.tukaani:xz:1.10")
    //noinspection GradleDependency
    implementation("androidx.core:core-ktx:1.12.0")
}

