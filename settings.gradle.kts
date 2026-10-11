pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "gielinorcore"

include(
    "buffer",
    "cache",
    "content",
    "transcode",
    "protocol",
    "game",
    "script",
    "server",
    "tools",
    "plugins:example",
)
