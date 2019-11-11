package esw.ocs.dsl.highlevel

interface AbortSequenceDsl {
    val commonUtils: CommonUtils

    suspend fun abortSequenceForSequencer(sequencerId: String, observingMode: String): Unit {
        commonUtils.sendMsgToSequencer(sequencerId, observingMode) { it.abortSequence() }
    }
}
