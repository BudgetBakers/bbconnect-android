// BBConnect Link SDK for Android (WP4.4). Toolchain versions are pinned in
// gradle/ once the wrapper is generated (see README — CI handoff).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "bbconnect-link"
include(":bbconnect", ":sample")
