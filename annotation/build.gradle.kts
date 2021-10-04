plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

task("javadocJar", type = Jar::class) {
    archiveClassifier.set("javadoc")

    from(tasks["javadoc"])
}