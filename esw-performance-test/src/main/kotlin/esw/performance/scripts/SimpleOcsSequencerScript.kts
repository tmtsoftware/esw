package esw.performance.scripts

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.IRIS
import kotlin.time.Duration
import kotlin.time.minutes
import kotlin.time.seconds

script {
    println("Loaded script successfully from examples!!!!")
    val irisSequencer = Sequencer(IRIS, ObsMode("perfTest"), Duration.minutes(5))
    println("Iris sequencer resolved successfully !!!!")

    onSetup("command-1") {
        val setupCommand = Setup("ESW.perf.test", "command-2")
        val sequence = sequenceOf(setupCommand)
        irisSequencer.submitAndWait(sequence, Duration.seconds(60))
    }
}