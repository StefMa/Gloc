plugins {
    kotlin("jvm") version ("1.2.71")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.10.0"
    id("maven-publish")
}

repositories {
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.ntrrgc:ts-generator:1.1.0")
    implementation("com.google.guava:guava:26.0-jre") { isTransitive = false }

    testImplementation("org.assertj:assertj-core:3.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
}

tasks.named<Test>("test").configure { useJUnitPlatform() }

group = "kotlin2ts"
version = "1.0.0"

pluginBundle {
    website = "https://github.com/alexvas/kotlin2typescript"
    vcsUrl = "https://github.com/alexvas/kotlin2typescript"
    tags = listOf("kotlin", "typescript")
}

gradlePlugin.plugins.create("kt2ts") {
    id = "kotlin2ts"
    displayName = "Kotlin into TypeScript converter"
    description = "A Gradle Plugin which translates Kotlin data classes into Typescript. It uses Alicia Boya García's TypeScript definition generator for that."
    implementationClass = "kotlin2ts.Kt2tsPlugin"
}

