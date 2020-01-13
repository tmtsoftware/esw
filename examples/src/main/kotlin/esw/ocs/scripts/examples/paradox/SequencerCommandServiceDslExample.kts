@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import esw.ocs.api.protocol.*
import esw.ocs.dsl.core.script
import kotlin.time.seconds

script {

    onSetup("setup-tcs") {

        // #creating-sequencer
        // create and resolve sequencer
        val tcsSequencer = Sequencer("TCS", "darknight", 5.seconds)
        // #creating-sequencer

        // #creating-sequence
        val hcdCommand: SequenceCommand = Setup("tcs", "setup-tcs-hcd")
        val assemblyCommand: SequenceCommand = Setup("tcs", "setup-tcs-assembly")
        val sequence: Sequence = sequenceOf(hcdCommand, assemblyCommand)
        // #creating-sequence

        // #submitAndQuery
        val submitResponse: SubmitResponse = tcsSequencer.submit(sequence)
        val queryResponse: SubmitResponse = tcsSequencer.query(submitResponse.runId())
        // #submitAndQuery

        // #queryFinal
        val finalResponse: SubmitResponse = tcsSequencer.queryFinal(submitResponse.runId())
        // #queryFinal

        // #queryFinalWithTimeout
        val finalRes: SubmitResponse = tcsSequencer.queryFinal(submitResponse.runId(), 5.seconds)
        // #queryFinalWithTimeout

        // #submitAndWait
        val sequenceResponse: SubmitResponse = tcsSequencer.submitAndWait(sequence)
        // #submitAndWait

        // #submitAndWaitWithTimeout
        val sequenceRes: SubmitResponse = tcsSequencer.submitAndWait(sequence, 5.seconds)
        // #submitAndWaitWithTimeout

        // #goOnline
        val response: GoOnlineResponse = tcsSequencer.goOnline()
        // #goOnline

        // #goOffline
        val offlineResponse: GoOfflineResponse = tcsSequencer.goOffline()
        // #goOffline

        // #diagnosticMode
        val diagnosticModeResponse: DiagnosticModeResponse = tcsSequencer.diagnosticMode(utcTimeNow(), "engineering")
        // #diagnosticMode

        // #operationsMode
        val operationsModeResponse: OperationsModeResponse = tcsSequencer.operationsMode()
        // #operationsMode
    }

}