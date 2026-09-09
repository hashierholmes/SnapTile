import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
}

val keystoreProperties = Properties()
val keystoreFile = rootProject.file("keystore.properties")

if (keystoreFile.exists()) {
    keystoreProperties.load(FileInputStream(keystoreFile))
}

android {

    namespace = "com.snaptile.app"
    compileSdk = 33

    defaultConfig {

        applicationId = "com.snaptile.app"
        minSdk = 21
        targetSdk = 33

        versionCode = 1
        versionName = "1.0.2"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {

        create("release") {

            val storeFilePath = keystoreProperties["STORE_FILE"] as String?
            val storePass = keystoreProperties["STORE_PASSWORD"] as String?
            val keyAlias = keystoreProperties["KEY_ALIAS"] as String?
            val keyPass = keystoreProperties["KEY_PASSWORD"] as String?

            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
            }

            if (storePass != null) storePassword = storePass
            if (keyAlias != null) this.keyAlias = keyAlias
            if (keyPass != null) keyPassword = keyPass
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {

        viewBinding = true
    }
}