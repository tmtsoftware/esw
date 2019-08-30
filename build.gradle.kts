import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.50"
}

group = "asda"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    /*
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0"
    compile group: 'org.jetbrains.kotlinx', name: 'kotlinx-coroutines-jdk8', version: '1.3.0'
    implementation 'com.github.tmtsoftware.csw:csw-params_2.13:1c2c2c4baaed0e14ae6b2dc579787b8f4b38616c'
    implementation 'com.github.tmtsoftware.esw:esw-ocs-impl_2.13:c964c43911dfbf88d0ffff10f9507f859d6eec2e'
     */

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