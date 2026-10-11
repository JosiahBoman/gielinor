plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":buffer"))
    implementation(project(":cache"))
    implementation(project(":content"))
    implementation(project(":protocol"))
    implementation(project(":game"))
    implementation(project(":script"))
    implementation(libs.netty.all)
    // Plugins are discovered through ServiceLoader at runtime.
    runtimeOnly(project(":plugins:example"))
}

application {
    mainClass.set("org.gielinorcore.server.MainKt")
}
