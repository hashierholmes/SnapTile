plugins {
    id("com.android.application")
}

/**
 * Control app for the SnapTile bank/UPI accessibility investigation.
 *
 * The screenshots that started this showed super.money warning about SnapTile
 * (sideloaded) while leaving Bitwarden and Key Mapper (both from Play) alone,
 * even though Key Mapper declares window content, gesture injection and key
 * event filtering. Two explanations survived, and they are confounded in that
 * data because every flagged app is also the only one declaring
 * accessibilityEventTypes="typeWindowsChanged":
 *
 *   1. provenance  - sideloaded + any enabled accessibility service is enough
 *   2. event types - the SDK keys on the event type SnapTile declares
 *
 * These two variants separate them. They are the same app, the same package
 * base, the same label style, the same (empty) service code; the *only*
 * difference is one attribute in res/xml/probe_service.xml:
 *
 *   minimal   - declares nothing but a description
 *   winchange - declares accessibilityEventTypes="typeWindowsChanged"
 *
 * Enable both, open super.money, and the warning tells us which -- or whether
 * neither -- is flagged. A probe app is disposable: uninstall it afterwards.
 */
android {
    namespace = "com.snaptile.a11yprobe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.snaptile.a11yprobe"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "config"

    productFlavors {
        // Baseline control: the most minimal accessibility service possible.
        create("minimal") {
            dimension = "config"
            applicationIdSuffix = ".minimal"
            versionNameSuffix = "-minimal"
            resValue("string", "probe_label", "A11yProbe A minimal")
            resValue("string", "probe_config", "declares no accessibilityEventTypes")
        }

        // Same app plus the one attribute unique to SnapTile in the comparison.
        create("winchange") {
            dimension = "config"
            applicationIdSuffix = ".winchange"
            versionNameSuffix = "-winchange"
            resValue("string", "probe_label", "A11yProbe B winchange")
            resValue("string", "probe_config", "declares accessibilityEventTypes=typeWindowsChanged")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
