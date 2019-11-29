package esw.ocs.dsl.epics

import esw.ocs.dsl.highlevel.EventSubscription

data class FSMSubscription(private val eventSubscription: EventSubscription, private val removeSubscriber: () -> Unit) {
    suspend fun cancel() {
        removeSubscriber()
        eventSubscription.cancel()
    }
}