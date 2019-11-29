package esw.ocs.dsl.epics

interface Refreshable {
    suspend fun refresh()
    fun addFSMSubscription(fsmSubscription: FSMSubscription)
}
