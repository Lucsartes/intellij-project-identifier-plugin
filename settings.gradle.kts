pluginManagement {
    repositories {
        // Resolve Gradle plugins from public repositories so the build works off the corporate network.
        // clear() drops any repositories injected by a global init script (e.g. ~/.gradle/init.d pointing at
        // an internal Nexus). Dependency repositories are declared separately in build.gradle.kts.
        clear()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "intellij-project-identifier-plugin"
