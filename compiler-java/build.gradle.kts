plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.0")

    implementation("com.google.auto.service:auto-service-annotations:1.0")
    implementation(project(":annotation"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType(JavaCompile::class) {
    options.apply {
        compilerArgs.addAll(
            listOf(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
            )
        )
    }
}