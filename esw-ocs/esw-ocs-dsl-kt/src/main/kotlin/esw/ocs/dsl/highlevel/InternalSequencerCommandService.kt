package esw.ocs.dsl.highlevel

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.*
import esw.ocs.dsl.script.utils.SequencerCommandServiceUtil
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletionStage

class InternalSequencerCommandService(private val sequencerCommandServiceUtil: SequencerCommandServiceUtil) {

    suspend fun submitAndWait(sequence: Sequence): SubmitResponse? = sequencerCommandServiceUtil.submitAndWait(sequence).await()
    suspend fun queryFinal(): SubmitResponse? = sequencerCommandServiceUtil.queryFinal().await()

    suspend fun submit(sequence: Sequence): OkOrUnhandledResponse? = sequencerCommandServiceUtil.submit(sequence).await()

    suspend fun goOnline(): GoOnlineResponse? = sequencerCommandServiceUtil.goOnline().await()
    suspend fun goOffline(): GoOfflineResponse? = sequencerCommandServiceUtil.goOffline().await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse? =
            sequencerCommandServiceUtil.diagnosticMode(startTime, hint).await()

    suspend fun operationsMode(): OperationsModeResponse? = sequencerCommandServiceUtil.operationsMode().await()
    suspend fun abortSequence(): OkOrUnhandledResponse? = sequencerCommandServiceUtil.abortSequence().await()

}