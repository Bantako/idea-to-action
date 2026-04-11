plugins {
    alias(libs.plugins.app.android.application)
    alias(libs.plugins.app.android.compose)
    alias(libs.plugins.app.android.hilt)
    alias(libs.plugins.app.android.room)
}

android {
    namespace = "org.mrlem.composesample"

    defaultConfig {
        applicationId = "org.mrlem.composesample"
        targetSdk = libs.versions.target.sdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val anthropicKey = project.findProperty("anthropic.api.key") as? String ?: ""
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$anthropicKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":theme"))
    implementation("androidx.activity:activity-compose")
    implementation(libs.okhttp)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.material.icons)
    implementation(libs.kotlin.coroutines)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
