package esw.ocs.dsl.highlevel.internal

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.*
import esw.ocs.dsl.script.utils.SequencerCommandServiceUtil
import kotlinx.coroutines.future.await

class InternalSequencerCommandService(
        private val sequencerId: String,
        private val observingMode: String,
        private val sequencerCommandServiceUtil: SequencerCommandServiceUtil
) {

    suspend fun submitAndWait(sequence: Sequence): SubmitResponse? =
            sequencerCommandServiceUtil.submitAndWait(sequencerId, observingMode, sequence).await()

    suspend fun goOnline(): GoOnlineResponse? = sequencerCommandServiceUtil.goOnline(sequencerId, observingMode).await()
    suspend fun goOffline(): GoOfflineResponse? = sequencerCommandServiceUtil.goOffline(sequencerId, observingMode).await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse? =
            sequencerCommandServiceUtil.diagnosticMode(sequencerId, observingMode, startTime, hint).await()

    suspend fun operationsMode(): OperationsModeResponse? = sequencerCommandServiceUtil.operationsMode(sequencerId, observingMode).await()
    suspend fun abortSequence(): OkOrUnhandledResponse? = sequencerCommandServiceUtil.abortSequence(sequencerId, observingMode).await()
    suspend fun stop(): OkOrUnhandledResponse? = sequencerCommandServiceUtil.stop(sequencerId, observingMode).await()

}