package esw.ocs.scripts.examples

import csw.params.events.EventName
import csw.params.events.ObserveEvent
import esw.ocs.dsl.highlevel.models.Prefix

object SharedConstants {
    object prefixes {
        val counter = Prefix("esw.counter")
    }

    object events {
        val getCounter = ObserveEvent(prefixes.counter, EventName("get-counter"))
    }
}