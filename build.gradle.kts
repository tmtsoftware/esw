import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.50" apply false
    `maven-publish`
}

allprojects {
    group = "com.github.tmtsoftware.esw"
    version = "0.1-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.gradle.maven-publish")

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("com.github.tmtsoftware.esw:esw-ocs-app_2.13:0.1-SNAPSHOT")
        implementation("com.github.tmtsoftware.csw:csw-params_2.13:0.1-SNAPSHOT")
        compile("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.0")
        compile("org.jetbrains.kotlin", "kotlin-script-runtime", "1.3.50")

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

    task<Jar>("sourcesJar") {
        from(project.the<SourceSetContainer>()["main"].java)
        archiveClassifier.set("sources")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = project.name
                from(components["java"])
            }
        }
    }
}

dependencies {
    // Make the root project archive configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}
