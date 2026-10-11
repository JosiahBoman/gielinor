plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":cache"))
    implementation(project(":content"))
    implementation(project(":transcode"))
}

application {
    mainClass.set("org.gielinorcore.tools.MainKt")
}
