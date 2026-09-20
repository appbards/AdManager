plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.appbards.admanager"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.appbards.admanager"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

configurations.all {
    // The GMA Next-Gen SDK bundles its own copy of the legacy ad classes; mediation
    // adapters still pull play-services-ads(-lite) in transitively, which would cause
    // duplicate-symbol errors. These excludes do not travel with a published AAR, so
    // every consuming app must repeat them.
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    // Core library (required)
//    implementation("com.github.AppBards.AdManager:admanager-core:1.0.0")

    // IronSource provider
//    implementation("com.github.AppBards.AdManager:admanager-ironsource:1.0.0")

    implementation(project(":admanager-core"))
//    implementation(project(":admanager-ironsource"))
    implementation(project(":admanager-admob"))

    // Coroutines (if not already present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
