package esw.ocs.scripts.examples.paradox

import csw.params.commands.Sequence
import esw.ocs.dsl.core.script
import kotlin.time.seconds

// script for ocs sequencer
script {

    onSetup("setup-tcs") {

        // #creating-sequencer
        // create and resolve sequencer
        val tcsSequencer = Sequencer("TCS", "darknight", 5.seconds)
        // #creating-sequencer

        // #submit
        val setupHcdCommand = Setup("tcs", "setup-hcd")
        val setupAssemblyCommand = Setup("tcs", "setup-assembly")
        val sequence = Sequence.create(listOf(setupHcdCommand, setupAssemblyCommand))
        val submitResponse = tcsSequencer.submit(sequence)
        val finalResponse = tcsSequencer.queryFinal(submitResponse.runId())
        // #submit

        // #submitAndWait
        val sequenceResponse = tcsSequencer.submitAndWait(sequence)
        // #submitAndWait


    }

}