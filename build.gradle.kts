plugins {
    kotlin("jvm") version ("1.2.71")
    id("java-gradle-plugin")
    id("maven-publish")
}

repositories {
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.ntrrgc:ts-generator:1.1.0")
    implementation("com.google.guava:guava:23.0") { isTransitive = false }

    testImplementation("org.assertj:assertj-core:3.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
}

(tasks.findByName("test") as Test).useJUnitPlatform()

tasks.register("manualClasspathCompilation") {
    val outputDir = file("$buildDir/$name")

    inputs.files(sourceSets["main"].runtimeClasspath)
    outputs.dir(outputDir)

    doLast {
        outputDir.mkdirs()
        file("$outputDir/plugin-classpath.txt").writeText(sourceSets["main"].runtimeClasspath.joinToString("\n"))
    }
}

group = "kotlin2ts"
version = "0.0.1"
gradlePlugin {
    plugins {
        create("kt2ts") {
            id = "kotlin2ts"
            implementationClass = "kotlin2ts.Kt2tsPlugin"
        }
    }
}