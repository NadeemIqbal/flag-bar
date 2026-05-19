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
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenLocal()       // for resolving debug-bar before its Central release hits CDN
        mavenCentral()
    }
}

rootProject.name = "flag-bar"

include(":flag-bar")
include(":sample:composeApp")
include(":sample:androidApp")
include(":sample:desktopApp")
include(":sample:webApp")
