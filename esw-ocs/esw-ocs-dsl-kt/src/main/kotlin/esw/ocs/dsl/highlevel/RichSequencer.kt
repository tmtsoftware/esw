package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.ControlCommand
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.protocol.*
import esw.ocs.dsl.jdk.toJava
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import kotlin.time.Duration

class RichSequencer(
        private val sequencerId: String,
        private val observingMode: String,
        private val sequencerApiFactory: BiFunction<String, String, CompletionStage<SequencerApi>>
) {

    private suspend fun sequencerAdmin() = sequencerApiFactory.apply(sequencerId, observingMode).await()

    suspend fun submit(sequence: Sequence): SubmitResponse = sequencerAdmin().submit(sequence).toJava().await()
    suspend fun query(runId: Id): CommandResponse.QueryResponse = sequencerAdmin().query(runId).toJava().await()

    suspend fun queryFinal(runId: Id, timeout: Duration): SubmitResponse {
        val akkaTimeout = Timeout(timeout.toLongNanoseconds(), TimeUnit.NANOSECONDS)
        return sequencerAdmin().queryFinal(runId, akkaTimeout).toJava().await()
    }

    suspend fun submitAndWait(sequence: Sequence, timeout: Duration): SubmitResponse? {
        val akkaTimeout = Timeout(timeout.toLongNanoseconds(), TimeUnit.NANOSECONDS)
        return sequencerAdmin().submitAndWait(sequence, akkaTimeout).toJava().await()
    }

    suspend fun goOnline(): GoOnlineResponse? =
            sequencerAdmin().goOnline().toJava().await()

    suspend fun goOffline(): GoOfflineResponse? =
            sequencerAdmin().goOffline().toJava().await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse? =
            sequencerAdmin().diagnosticMode(startTime, hint).toJava().await()

    suspend fun operationsMode(): OperationsModeResponse? =
            sequencerAdmin().operationsMode().toJava().await()

    suspend fun abortSequence(): OkOrUnhandledResponse? =
            sequencerAdmin().abortSequence().toJava().await()

    suspend fun stop(): OkOrUnhandledResponse? =
            sequencerAdmin().stop().toJava().await()

}
