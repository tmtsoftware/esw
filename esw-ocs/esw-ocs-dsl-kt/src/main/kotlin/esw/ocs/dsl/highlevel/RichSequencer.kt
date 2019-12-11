package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.params.core.models.Subsystem
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.protocol.*
import esw.ocs.dsl.jdk.SuspendToJavaConverter
import esw.ocs.dsl.jdk.toJava
import esw.ocs.impl.SequencerActorProxyFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import kotlin.time.Duration

class RichSequencer(
        internal val subsystem: Subsystem,
        private val observingMode: String,
        private val sequencerApiFactory: SequencerActorProxyFactory,
        override val coroutineScope: CoroutineScope
) : SuspendToJavaConverter {

    private suspend fun sequencerAdmin() = sequencerApiFactory.jMake(subsystem, observingMode).await()

    suspend fun submit(sequence: Sequence): SubmitResponse = sequencerAdmin().submit(sequence).toJava().await()
    suspend fun query(runId: Id): SubmitResponse = sequencerAdmin().query(runId).toJava().await()

    suspend fun queryFinal(runId: Id, timeout: Duration): SubmitResponse {
        val akkaTimeout = Timeout(timeout.toLongNanoseconds(), TimeUnit.NANOSECONDS)
        return sequencerAdmin().queryFinal(runId, akkaTimeout).toJava().await()
    }

    suspend fun submitAndWait(sequence: Sequence, timeout: Duration, resumeOnError: Boolean = false): SubmitResponse {
        val akkaTimeout = Timeout(timeout.toLongNanoseconds(), TimeUnit.NANOSECONDS)
        val submitResponse: SubmitResponse = sequencerAdmin().submitAndWait(sequence, akkaTimeout).toJava().await()
        if (!resumeOnError && CommandResponse.isNegative(submitResponse)) throw SubmitError(submitResponse)
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
