plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// کلیدهای تپسل از gradle.properties خوانده می‌شوند تا داخل کد hardcode نشوند
val tapsellAppId: String = (project.findProperty("TAPSELL_APP_ID") ?: "") as String
val tapsellInterstitialZone: String = (project.findProperty("TAPSELL_ZONE_INTERSTITIAL") ?: "") as String
val tapsellBannerZone: String = (project.findProperty("TAPSELL_ZONE_BANNER") ?: "") as String

android {
    namespace = "ir.amir.applimiter"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.amir.applimiter"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        // App ID تپسل از طریق manifest placeholder به SDK داده می‌شود
        addManifestPlaceholders(
            mapOf("TapsellMediationAppKey" to tapsellAppId)
        )

        buildConfigField("String", "TAPSELL_ZONE_INTERSTITIAL", "\"$tapsellInterstitialZone\"")
        buildConfigField("String", "TAPSELL_ZONE_BANNER", "\"$tapsellBannerZone\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val tapsellVersion = "1.3.0"

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ---- تپسل مدیشن ----
    implementation("ir.tapsell:tapsell:$tapsellVersion")
    implementation("ir.tapsell.mediation.adapter:legacy:$tapsellVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
