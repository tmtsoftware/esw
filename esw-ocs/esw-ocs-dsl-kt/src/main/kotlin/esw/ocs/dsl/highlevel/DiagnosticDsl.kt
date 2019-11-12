package esw.ocs.dsl.highlevel

import csw.time.core.models.UTCTime

interface DiagnosticDsl {

    val commonUtils: CommonUtils

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
