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
rootProject.name = "Photodine"
include(":app")
include(":core:engine")
include(":core:ui")
include(":feature:canvas")
include(":feature:layers")
include(":feature:tools")
include(":feature:colorpicker")
include(":feature:export")
