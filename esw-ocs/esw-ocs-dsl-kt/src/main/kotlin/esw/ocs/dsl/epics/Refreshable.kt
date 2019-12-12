package esw.ocs.dsl.epics

interface Refreshable {
    suspend fun refresh()
    fun addFsmSubscription(fsmSubscription: FsmSubscription)
}
