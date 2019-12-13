package esw.ocs.scripts.examples

import csw.params.events.EventName
import csw.params.events.ObserveEvent
import csw.prefix.models.Prefix

object SharedConstants {
    object prefixes {
        val counter = Prefix.apply("esw.counter")
    }

    object events {
        val getCounter = ObserveEvent(prefixes.counter, EventName("get-counter"))
    }
}