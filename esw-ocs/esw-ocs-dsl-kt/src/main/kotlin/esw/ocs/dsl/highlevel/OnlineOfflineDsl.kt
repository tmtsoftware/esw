package esw.ocs.dsl.highlevel

interface OnlineOfflineDsl {

    val commonUtils: CommonUtils

    suspend fun goOnlineModeForSequencer(sequencerId: String, observingMode: String): Unit =
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) { it.goOnline() }

    suspend fun goOfflineModeForSequencer(sequencerId: String, observingMode: String): Unit =
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) { it.goOffline() }
}
