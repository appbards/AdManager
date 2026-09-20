plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.appbards.admanager.admob"
    compileSdk {
        // 36 required: bundled AdMob adapters pull androidx.browser:browser:1.9.0,
        // which requires compileSdk 36+.
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    exclude(group = "com.unity3d.ads-mediation", module = "adquality-sdk")

    // The GMA Next-Gen SDK bundles its own copy of the legacy ad classes. Mediation
    // adapters still declare play-services-ads(-lite) transitively, which would pull
    // in duplicate symbols, so strip them everywhere.
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    // AdManager Core
    api(project(":admanager-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Mobile Ads SDK (Next-Gen)
    implementation(libs.ads.mobile.sdk)

    // UMP / GDPR consent SDK
    implementation(libs.user.messaging.platform)

    // AdMob mediation adapters
    implementation(libs.admob.adapter.ironsource)
    implementation(libs.admob.adapter.unity)
    implementation(libs.admob.adapter.chartboost)
    implementation(libs.admob.adapter.liftoff)
    implementation(libs.admob.adapter.meta)
    implementation(libs.admob.adapter.mintegral)

    // Unity also requires the underlying SDK declared explicitly
    implementation(libs.unity.ads)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.appbards"
                artifactId = "admanager-admob"
                version = "1.0.0"
            }
        }
    }
}