import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Own signing key so the app can always be updated in place (PLAN Phase 0).
// keystore.properties (repo root, gitignored): storeFile, storePassword, keyAlias, keyPassword.
val keystorePropertiesFile = rootProject.file("keystore.properties")

android {
    namespace = "hr.podsjetnik"
    compileSdk = 37

    defaultConfig {
        applicationId = "hr.podsjetnik"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            val props = Properties().apply { keystorePropertiesFile.inputStream().use { load(it) } }
            create("podsjetnik") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // ponytail: falls back to the default debug key until keystore.properties exists
        val ownKey = signingConfigs.findByName("podsjetnik")
        debug {
            if (ownKey != null) signingConfig = ownKey
        }
        release {
            isMinifyEnabled = false
            signingConfig = ownKey ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
