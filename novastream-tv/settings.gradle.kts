pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven {
            url =
                java.net.URI("https://artifactory.yandex.net/artifactory/yandex_mobile_sdk_maven/")
        }
    }
}

rootProject.name = "NovaStream - Live TV"
include(":app")