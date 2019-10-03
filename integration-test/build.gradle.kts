import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
}

dependencies {
    testImplementation("com.github.tmtsoftware.csw:csw-testkit_2.13:65fc8ae")
    testImplementation(project(":examples"))
}