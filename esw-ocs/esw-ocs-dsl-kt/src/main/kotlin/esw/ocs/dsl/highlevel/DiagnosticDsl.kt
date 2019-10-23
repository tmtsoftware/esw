package esw.ocs.dsl.highlevel

import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.DiagnosticDataMessage.`OperationsMode$`
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime

interface DiagnosticDsl {

    val commonUtils: CommonUtils

    suspend fun diagnosticModeForComponent(
        componentName: String,
        componentType: ComponentType,
        startTime: UTCTime,
        hint: String
    ): Unit = commonUtils.sendMsgToComponent(componentName, componentType) {
        it.tell(DiagnosticMode(startTime, hint))
    }

    suspend fun operationsModeForComponent(componentName: String, componentType: ComponentType): Unit =
        commonUtils.sendMsgToComponent(componentName, componentType) {
            it.tell(`OperationsMode$`.`MODULE$`)
        }

    suspend fun diagnosticModeForSequencer(
        sequencerId: String,
        observingMode: String,
        startTime: UTCTime,
        hint: String
    ): Unit = commonUtils.sendMsgToSequencer(sequencerId, observingMode) {
        it.diagnosticMode(startTime, hint)
    }

    suspend fun operationsModeForSequencer(sequencerId: String, observingMode: String): Unit =
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) {
            it.operationsMode()
        }
}
