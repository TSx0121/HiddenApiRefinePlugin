plugins {
    id("org.jetbrains.intellij") version "1.2.0"
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2020.3.1")
    plugins.set(listOf("java"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":annotation"))
}

tasks {
    patchPluginXml {

    }
}