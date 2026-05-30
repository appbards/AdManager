plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.appbards.admanager.admob"
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
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

    // Google Mobile Ads SDK
    implementation(libs.play.services.ads)

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