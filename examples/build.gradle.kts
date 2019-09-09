plugins {
    application
}

application {
    mainClassName = "esw.ocs.app.SequencerApp"
}

dependencies {
    implementation(project(":script-dsl"))
}
