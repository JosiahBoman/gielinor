plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":cache"))
    implementation(libs.kotlinx.serialization.json)
}
