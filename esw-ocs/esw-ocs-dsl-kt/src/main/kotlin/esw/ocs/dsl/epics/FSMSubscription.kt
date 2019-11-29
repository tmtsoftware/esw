package esw.ocs.dsl.epics

import esw.ocs.dsl.highlevel.Subscription

data class FSMSubscription(private val eventSubscription: Subscription, private val remove: () -> Unit) {
    suspend fun cancel() {
        remove()
        eventSubscription.cancel()
    }
}