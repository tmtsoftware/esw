@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.SubmitResponse
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
        val sequence: Sequence = sequenceOf(setupHcdCommand, setupAssemblyCommand)
        val submitResponse: SubmitResponse = tcsSequencer.submit(sequence)
        val finalResponse: SubmitResponse = tcsSequencer.queryFinal(submitResponse.runId())
        // #submit

        // #submitAndWait
        val sequenceResponse: SubmitResponse = tcsSequencer.submitAndWait(sequence)
        // #submitAndWait


    }

}