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
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.videolan.org/maven") }
        maven { url = uri("https://maven.mango-cpe.net/") } // libmpv/mpv-android artifacts
        maven { url = uri("https://github.com/tvbox1201/mpv-android-maven/raw/mvn-repo") }
    }
}

rootProject.name = "DylandosIPTV"
include(":app")
