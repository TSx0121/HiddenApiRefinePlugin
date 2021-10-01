plugins {
    java
    `java-gradle-plugin`
}

val pluginId = "$group.$name"
val pluginClass = "$group.RefinePlugin"

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:7.0.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    plugins {
        create("HiddenApiRefine") {
            id = pluginId
            implementationClass = pluginClass
        }
    }
}
