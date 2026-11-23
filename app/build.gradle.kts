plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    signingConfigs {
        create("fixed") {
            storeFile = rootProject.file("keystore/dnschanger.jks")
            storePassword = "dnschanger123"
            keyAlias = "dnschanger"
            keyPassword = "dnschanger123"
        }
    }
    namespace = "com.dnschanger.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.dnschanger.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "1.11"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("fixed") }
    }
    kotlinOptions { jvmTarget = "17" }
}
