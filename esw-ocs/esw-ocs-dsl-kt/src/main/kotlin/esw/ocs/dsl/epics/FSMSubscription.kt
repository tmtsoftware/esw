package esw.ocs.dsl.epics

data class FSMSubscription(private val unsubscribe: suspend () -> Unit) {
    suspend fun cancel() = unsubscribe()
}