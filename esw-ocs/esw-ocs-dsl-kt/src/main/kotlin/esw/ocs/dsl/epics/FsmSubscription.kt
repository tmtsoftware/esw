package esw.ocs.dsl.epics

data class FsmSubscription(private val unsubscribe: suspend () -> Unit) {
    suspend fun cancel() = unsubscribe() // to solve mocking issue, we had to introduce this function: https://github.com/mockk/mockk/issues/288
}