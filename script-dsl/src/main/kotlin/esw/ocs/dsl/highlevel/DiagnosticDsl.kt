package esw.ocs.dsl.highlevel

import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.DiagnosticDataMessage.`OperationsMode$`
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import kotlinx.coroutines.future.await

interface DiagnosticDsl {

    val sequencerAdminFactory: SequencerAdminFactoryApi

    val locationServiceUtil: LocationServiceUtil

    suspend fun diagnosticModeForComponent(
        componentName: String,
        componentType: ComponentType,
        startTime: UTCTime,
        hint: String
    ): Unit = sendMsgToComponent(componentName, componentType, DiagnosticMode(startTime, hint))

    suspend fun operationsModeForComponent(
        componentName: String,
        componentType: ComponentType
    ): Unit = sendMsgToComponent(componentName, componentType, `OperationsMode$`.`MODULE$`)

    suspend fun diagnosticModeForSequencer(
        sequencerId: String,
        observingMode: String,
        startTime: UTCTime,
        hint: String
    ): Unit = sendMsgToSequencer(sequencerId, observingMode) { it.diagnosticMode(startTime, hint) }

    suspend fun operationsModeForSequencer(
        sequencerId: String,
        observingMode: String
    ): Unit = sendMsgToSequencer(sequencerId, observingMode) { it.operationsMode() }

    private suspend fun sendMsgToSequencer(
        sequencerId: String,
        observingMode: String,
        action: (SequencerAdminApi) -> Unit
    ): Unit = action(sequencerAdminFactory.jMake(sequencerId, observingMode).await())

    private suspend fun sendMsgToComponent(
        componentName: String,
        componentType: ComponentType,
        msg: DiagnosticDataMessage
    ): Unit = locationServiceUtil.jResolveComponentRef(componentName, componentType).await().tell(msg)
}
