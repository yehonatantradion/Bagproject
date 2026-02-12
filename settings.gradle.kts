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
        // ✅ הוספתי את השורה הזו כדי לאפשר הורדת ספריות מ-GitHub/JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "My Application"
include(":app")