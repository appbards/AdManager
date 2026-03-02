pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()


    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Chartboost
        maven(url = "https://cboost.jfrog.io/artifactory/chartboost-ads/")

        // Mintegral / MBridge (Oversea)
        maven(url = "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea/")
    }
}

rootProject.name = "AdManager"
include(":app")
include(":admanager-core")
include(":admanager-ironsource")
