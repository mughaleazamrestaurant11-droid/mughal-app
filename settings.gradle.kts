// Mughal Traders - mobile client (Android).
// The app is a thin Kotlin shell around the shop system that lives on the
// MAIN PC: it finds the main PC on the LAN, loads its UI and follows it in
// real time. The phone never stores the shop data itself - the Main PC is
// always the Main PC.

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
    }
}

rootProject.name = "MughalTraders"
include(":app")
