package esw.ocs.scripts.examples

import csw.params.events.EventName
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.highlevel.models.Prefix

object SharedConstants {
    object prefixes {
        val counter = Prefix("ESW.counter")
    }

    object events {
        val getCounter = SystemEvent(prefixes.counter, EventName("get-counter"))
    }
}
