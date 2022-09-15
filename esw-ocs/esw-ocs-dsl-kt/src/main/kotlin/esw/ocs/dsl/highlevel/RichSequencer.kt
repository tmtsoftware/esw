/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.api.models.Variation
import esw.ocs.api.protocol.*
import esw.ocs.dsl.highlevel.models.CommandError
import esw.ocs.dsl.isFailed
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.jdk.toJava
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class RichSequencer(
        internal val subsystem: Subsystem,
        private val obsMode: ObsMode,
        private val variation: Variation?,
        private val sequencerApiFactory: (Subsystem, ObsMode, Variation?) -> CompletionStage<SequencerApi>,
        private val defaultTimeout: Duration,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {

    private suspend fun sequencerService() = sequencerApiFactory(subsystem, obsMode, variation).await()

    /**
     * Submit a sequence to the sequencer and return the immediate response If it returns as `Started` get a
     * final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future with queryFinal.
     *
     * @param sequence the [[csw.params.commands.Sequence]] payload
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a [[csw.params.commands.CommandResponse.SubmitResponse]] response
     */
    suspend fun submit(sequence: Sequence, resumeOnError: Boolean = false): SubmitResponse {
        val submitResponse: SubmitResponse = sequencerService().submit(sequence).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    /**
     * Query for the result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]]
     *
     * @param runId the runId of the sequence for which response is required
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a [[csw.params.commands.CommandResponse.SubmitResponse]] response
     */
    suspend fun query(runId: Id, resumeOnError: Boolean = false): SubmitResponse {
        val submitResponse: SubmitResponse = sequencerService().query(runId).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    /**
     * Query for the final result of a long running sequence which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]]
     *
     * @param runId the runId of the sequence for which response is required
     * @param timeout duration for which api will wait for final response, if command is not completed queryFinal will timeout
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a [[csw.params.commands.CommandResponse.SubmitResponse]] response
     */
    suspend fun queryFinal(runId: Id, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse {
        val akkaTimeout = Timeout(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        val submitResponse: SubmitResponse = sequencerService().queryFinal(runId, akkaTimeout).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    /**
     * Submit a sequence and wait for the final result to get a
     * final [[csw.params.commands.CommandResponse.SubmitResponse]]
     *
     * @param sequence the [[csw.params.commands.Sequence]] payload
     * @param timeout duration for which api will wait for final response
     * @param resumeOnError script execution continues if set true. If false, script execution flow breaks and sequence in
     * execution completes with failure.
     * @return a [[csw.params.commands.CommandResponse.SubmitResponse]] response
     */
    suspend fun submitAndWait(sequence: Sequence, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse {
        val akkaTimeout = Timeout(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        val submitResponse: SubmitResponse = sequencerService().submitAndWait(sequence, akkaTimeout).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    /**
     * Sends the sequencer in Online Mode if it is Offline and executes the onGoOnline handler of the script
     *
     * @return a [[esw.ocs.api.protocol.GoOnlineResponse]] response
     */
    suspend fun goOnline(): GoOnlineResponse =
            sequencerService().goOnline().toJava().await()

    /**
     * Sends the sequencer in Offline Mode if it is Online and executes the onGoOffline handler of the script
     *
     * @return a [[esw.ocs.api.protocol.GoOfflineResponse]] response
     */
    suspend fun goOffline(): GoOfflineResponse =
            sequencerService().goOffline().toJava().await()

    /**
     * Runs the onDiagnosticMode handler of script
     *
     * @param startTime - startTime argument for the diagnostic handler
     * @param hint - hint argument for the diagnostic handler
     *
     * @return a [[esw.ocs.api.protocol.DiagnosticModeResponse]] response
     */
    suspend fun diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse =
            sequencerService().diagnosticMode(startTime, hint).toJava().await()

    /**
     * Runs the onOperationsMode handler of script
     *
     * @return a [[esw.ocs.api.protocol.OperationsModeResponse]] response
     */
    suspend fun operationsMode(): OperationsModeResponse =
            sequencerService().operationsMode().toJava().await()

    /**
     * Aborts the running sequence in the sequencer by discarding all the pending steps and runs the onAbortSequence handler of the script
     *
     * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] response
     */
    suspend fun abortSequence(): OkOrUnhandledResponse =
            sequencerService().abortSequence().toJava().await()

    /**
     * Stops the running sequence in the sequencer by discarding all the pending steps and runs the onStop handler of the script
     *
     * @return an [[esw.ocs.api.protocol.OkOrUnhandledResponse]] response
     */
    suspend fun stop(): OkOrUnhandledResponse =
            sequencerService().stop().toJava().await()

}
