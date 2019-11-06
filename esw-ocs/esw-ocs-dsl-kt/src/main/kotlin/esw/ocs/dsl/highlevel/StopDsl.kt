package esw.ocs.dsl.highlevel

interface StopDsl {
    val commonUtils: CommonUtils

    suspend fun stopSequencer(sequencerId: String, observingMode: String): Unit =
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) { it.stop() }
}
