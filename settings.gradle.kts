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
        // مخزن تپسل
        maven { url = uri("https://maven.tapsell.ir") }
        google()
        mavenCentral()
    }
}
rootProject.name = "AppLimiter"
include(":app")
