import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.50" apply false
    `maven-publish`
    id("org.jmailen.kotlinter") version "2.1.1"
}

allprojects {
    group = "com.github.tmtsoftware.script-dsl"
    version = "0.1-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
        maven("https://plugins.gradle.org/m2/")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.jmailen.kotlinter")

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("com.github.tmtsoftware.esw:esw-ocs-app_2.13:aa52a99510e5045f9957e797aa60593c67097522")
        compile("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.0")
        compile("org.jetbrains.kotlin", "kotlin-script-runtime", "1.3.50")

        testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")
        testCompile("junit", "junit", "4.12")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
//            freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
        }
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.register<Jar>("sourcesJar") {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }

    tasks.register<Jar>("javadocJar") {
        from(tasks.javadoc)
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
            }
        }
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }

    kotlinter {
        disabledRules = arrayOf("no-wildcard-imports")
    }
}

dependencies {
    // Make the root project archive configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}
