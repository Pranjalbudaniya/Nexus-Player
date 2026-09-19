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
    }
}

rootProject.name = "NexusPlayer"

include(":app")
include(":core:common")
include(":core:database")
include(":core:designsystem")
include(":core:media")
include(":core:navigation")
include(":core:player")
include(":core:scanner")
include(":core:ui")
include(":feature:home")
include(":feature:library")
include(":feature:onboarding")
include(":feature:player")
include(":feature:playlists")
include(":feature:search")
