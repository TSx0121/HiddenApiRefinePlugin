plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":annotation"))

    annotationProcessor("com.google.auto.service:auto-service:1.0")
    implementation("com.google.auto.service:auto-service-annotations:1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType(JavaCompile::class) {
    options.apply {
        compilerArgs.addAll(listOf("--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED"))
        compilerArgs.addAll(listOf("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"))
        compilerArgs.addAll(listOf("--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED"))
        compilerArgs.addAll(listOf("--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"))
    }
}