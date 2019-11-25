package esw.ocs.dsl.highlevel

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.api.protocol.*
import esw.ocs.dsl.jdk.toJava
import kotlinx.coroutines.future.await

class RichSequencer(
        private val sequencerId: String,
        private val observingMode: String,
        private val sequencerAdminFactory: SequencerAdminFactoryApi
) {

    private suspend fun sequencerAdmin() =
            sequencerAdminFactory.jMake(sequencerId, observingMode).await()

    suspend fun submitAndWait(sequence: Sequence): SubmitResponse? =
            sequencerAdmin().submitAndWait(sequence).toJava().await()

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
