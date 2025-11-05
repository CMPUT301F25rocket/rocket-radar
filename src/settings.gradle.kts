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
        // Arcane issues. This is for a dep of the bottom sheet calendar picker.
        // https://github.com/kizitonwose/Calendar/issues/413#issuecomment-1298051528
        maven("https://jitpack.io/")
    }
}

rootProject.name = "Radar"
include(":app")
