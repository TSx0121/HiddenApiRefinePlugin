plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(project(":compiler-java"))
    compileOnly(project(":annotation"))
}

tasks.withType(JavaCompile::class) {
    val additionCompilerArgs = listOf(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
    )

    options.apply {
        isFork = true
        forkOptions.apply {
            jvmArgs = additionCompilerArgs
        }
    }
}

