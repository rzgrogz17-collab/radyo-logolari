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
        // Huawei Petal Ads (HMS Ads Kit) SDK'sı sadece bu depoda yayınlanıyor.
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "MahjongYandex"
include(":app")
