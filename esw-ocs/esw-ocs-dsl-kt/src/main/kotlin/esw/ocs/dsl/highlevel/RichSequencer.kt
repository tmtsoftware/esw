package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
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
        private val sequencerApiFactory: (Subsystem, ObsMode) -> CompletionStage<SequencerApi>,
        private val defaultTimeout: Duration,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {

    private suspend fun sequencerAdmin() = sequencerApiFactory(subsystem, obsMode).await()

    suspend fun submit(sequence: Sequence, resumeOnError: Boolean = false): SubmitResponse {
        val submitResponse: SubmitResponse = sequencerAdmin().submit(sequence).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    suspend fun query(runId: Id, resumeOnError: Boolean = false): SubmitResponse {
        val submitResponse: SubmitResponse = sequencerAdmin().query(runId).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    suspend fun queryFinal(runId: Id, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse {
        val akkaTimeout = Timeout(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        val submitResponse: SubmitResponse = sequencerAdmin().queryFinal(runId, akkaTimeout).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    suspend fun submitAndWait(sequence: Sequence, timeout: Duration = defaultTimeout, resumeOnError: Boolean = false): SubmitResponse {
        val akkaTimeout = Timeout(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        val submitResponse: SubmitResponse = sequencerAdmin().submitAndWait(sequence, akkaTimeout).toJava().await()
        if (!resumeOnError && submitResponse.isFailed) throw CommandError(submitResponse)
        return submitResponse
    }

    suspend fun goOnline(): GoOnlineResponse =
            sequencerAdmin().goOnline().toJava().await()

    suspend fun goOffline(): GoOfflineResponse =
            sequencerAdmin().goOffline().toJava().await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse =
            sequencerAdmin().diagnosticMode(startTime, hint).toJava().await()

    suspend fun operationsMode(): OperationsModeResponse =
            sequencerAdmin().operationsMode().toJava().await()

    suspend fun abortSequence(): OkOrUnhandledResponse =
            sequencerAdmin().abortSequence().toJava().await()

    suspend fun stop(): OkOrUnhandledResponse =
            sequencerAdmin().stop().toJava().await()

}
