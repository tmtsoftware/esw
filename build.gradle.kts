import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-library`
    kotlin("jvm") version "1.3.50" apply false
    `maven-publish`
    id("org.jmailen.kotlinter") version "2.1.1"
    id("com.adarshr.test-logger") version "1.7.0"
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
    apply(plugin = "com.adarshr.test-logger")

    tasks.withType<Test> {
        useJUnitPlatform()
        testlogger {
            theme = ThemeType.MOCHA
        }
    }

    /**
     * Dependencies appearing in the api configurations will be transitively exposed to consumers of the library, and as
     * such will appear on the compile classpath of consumers. Dependencies found in the implementation configuration will,
     * on the other hand, not be exposed to consumers, and therefore not leak into the consumers' compile classpath
     */
    dependencies {
        api(kotlin("stdlib-jdk8"))
        implementation("com.github.tmtsoftware.esw:esw-ocs-app_2.13:1d56dbc98ecd2aa3c8952d259f1f8b9564efc0f5")
        api("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.0")
        api("org.jetbrains.kotlin", "kotlin-script-runtime", "1.3.50")

        testImplementation("io.mockk:mockk:1.9")
        testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")
        testImplementation("junit", "junit", "4.12")
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
