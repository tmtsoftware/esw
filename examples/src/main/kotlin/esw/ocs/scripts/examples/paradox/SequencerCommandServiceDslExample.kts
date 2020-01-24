@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import esw.ocs.api.protocol.*
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.WFOS
import kotlin.time.seconds

script {

    onSetup("setup-wfos") {

        // #creating-sequencer
        // create a sequencer entity
        val wfos = Sequencer(WFOS, "wfos_darknight", 5.seconds)
        // #creating-sequencer

        // #creating-sequence
        val wfosCommand1: SequenceCommand = Setup("ESW.wfos_darknight", "wfosCommand1")
        val wfosCommand2: SequenceCommand = Setup("ESW.wfos_darknight", "wfosCommand2")
        val sequence: Sequence = sequenceOf(wfosCommand1, wfosCommand2)
        // #creating-sequence

        // #submitAndQuery
        val submitResponse: SubmitResponse = wfos.submit(sequence)
        val queryResponse: SubmitResponse = wfos.query(submitResponse.runId())
        // #submitAndQuery

        // #resumeOnError
        val response: SubmitResponse = tcsSequencer.submit(sequence, resumeOnError = true)
        // #resumeOnError

        // #queryFinal
        val finalResponse: SubmitResponse = wfos.queryFinal(submitResponse.runId())
        // #queryFinal

        // #queryFinalWithTimeout
        val finalRes: SubmitResponse = wfos.queryFinal(submitResponse.runId(), 5.seconds)
        // #queryFinalWithTimeout

        // #submitAndWait
        val sequenceResponse: SubmitResponse = wfos.submitAndWait(sequence)
        // #submitAndWait

        // #submitAndWaitWithTimeout
        val sequenceRes: SubmitResponse = wfos.submitAndWait(sequence, 5.seconds)
        // #submitAndWaitWithTimeout

        // #goOnline
        val onlineResponse: GoOnlineResponse = wfos.goOnline()
        // #goOnline

        // #goOffline
        val offlineResponse: GoOfflineResponse = wfos.goOffline()
        // #goOffline

        // #diagnosticMode
        val diagnosticModeResponse: DiagnosticModeResponse = wfos.diagnosticMode(utcTimeNow(), "engineering")
        // #diagnosticMode

        // #operationsMode
        val operationsModeResponse: OperationsModeResponse = wfos.operationsMode()
        // #operationsMode

        // #abortSequence
        val abortResponse: OkOrUnhandledResponse = wfos.abortSequence()
        // #abortSequence

        // #stopSequence
        val stopResponse: OkOrUnhandledResponse = wfos.stop()
        // #stopSequence
    }

}