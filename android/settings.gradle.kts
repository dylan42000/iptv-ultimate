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
        // Jitpack now requires auth for many artifacts and returns 401 — keep but last resort
        maven { url = uri("https://jitpack.io") }
        // VideoLAN maven for libvlc-all. If you get "No such host is known repo.videolan.org"
        // run: ipconfig /flushdns and set DNS to 8.8.8.8 / 1.1.1.1
        maven { url = uri("https://repo.videolan.org/maven") }
        // REMOVED dead repos that cause build to hang/fail:
        // maven { url = uri("https://maven.mango-cpe.net/") } // DEAD - DNS fails
        // maven { url = uri("https://github.com/tvbox1201/mpv-android-maven/raw/mvn-repo") } // 404 - repo deleted
        // For MPV, use Maven Central reliable artifact instead:
        // io.github.abdallahmehiz:mpv-android-lib:0.1.12 (same package is.xyz.mpv)
    }
}

rootProject.name = "DylandosIPTV"
include(":app")
