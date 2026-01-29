plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.appbards.ai.landmark.admanager.ironsource"
    compileSdk {
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
    implementation("com.unity3d.ads-mediation:mediation-sdk:9.2.0")
    implementation("com.google.android.gms:play-services-appset:16.0.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
    implementation("com.google.android.gms:play-services-basement:18.1.0")
    // Add Meta Audience Network
    implementation("com.unity3d.ads-mediation:facebook-adapter:5.1.0")
    implementation("com.facebook.android:audience-network-sdk:6.21.0")
    // Add Unity Ads Network
    implementation("com.unity3d.ads-mediation:unityads-adapter:5.3.0")
    implementation("com.unity3d.ads:unity-ads:4.16.4")
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