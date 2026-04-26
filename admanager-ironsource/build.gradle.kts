plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.appbards.ai.landmark.admanager.ironsource"
    compileSdk {
        version = release(35)
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AdManager Core (our library)
    api(project(":admanager-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.coroutines.core)

    /*
     * Ironsource Mediation 9.2.0
     */
    implementation(libs.mediation.sdk)
    implementation(libs.play.services.appset)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.play.services.basement)
    // Add Meta Audience Network
    implementation(libs.facebook.adapter)
    implementation(libs.audience.network.sdk)
    // Add Unity Ads Network
    implementation(libs.unityads.adapter)
    implementation(libs.unity.ads)
    // Add Chartboost Network
    implementation(libs.chartboost.adapter)
    implementation(libs.chartboost.sdk)
    // Add Mintegral Network
    implementation(libs.mintegral.adapter)
    implementation(libs.mbridge.android.sdk)
}

// Publishing configuration
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.appbards"
                artifactId = "admanager-ironsource"
                version = "1.0.0"
            }
        }
    }
}