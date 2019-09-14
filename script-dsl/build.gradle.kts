import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
}