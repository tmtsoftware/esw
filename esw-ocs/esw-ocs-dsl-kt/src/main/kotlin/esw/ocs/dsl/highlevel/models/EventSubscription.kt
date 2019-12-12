package esw.ocs.dsl.highlevel.models

data class EventSubscription(private val unsubscribe: suspend () -> Unit) {
    suspend fun cancel() = unsubscribe() // to solve mocking issue, we had to introduce this function: https://github.com/mockk/mockk/issues/288
}