import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

plugins {
    java
    kotlin("jvm") version "1.3.50"
}

group = "com.github.tmtsoftware.esw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.tmtsoftware.esw:esw-ocs-impl_2.13:c964c43911dfbf88d0ffff10f9507f859d6eec2e")
    implementation("com.github.tmtsoftware.csw:csw-params_2.13:1c2c2c4baaed0e14ae6b2dc579787b8f4b38616c")
    compile("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.0")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "esw.ocs.dsl.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}