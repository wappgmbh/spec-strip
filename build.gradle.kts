import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "gmbh.wapp"
version = "1.0.0"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("spec-strip")
    archiveClassifier.set("")

    mergeServiceFiles()
}
application {
    mainClassName = "MainKt"
}

dependencies {
    implementation("com.google.guava:guava:29.0-jre")
    implementation("io.airlift:airline:0.9")
    implementation("io.swagger.parser.v3:swagger-parser:2.0.23")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}