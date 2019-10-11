import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
}

val scalaVersion = "2.13"
val akkaVersion = "2.5.25"
dependencies {
    testImplementation("com.github.tmtsoftware.csw:csw-testkit_${scalaVersion}:65fc8ae")
    testImplementation("com.typesafe.akka:akka-testkit_${scalaVersion}:${akkaVersion}")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_${scalaVersion}:${akkaVersion}")
    testImplementation(project(":examples"))
    testImplementation(project(":script-dsl"))
}