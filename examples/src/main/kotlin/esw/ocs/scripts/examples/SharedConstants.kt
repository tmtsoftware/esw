package esw.ocs.scripts.examples

import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.ObserveEvent

object SharedConstants {
    object prefixes {
        val counter = Prefix.apply("esw.counter")
    }

    object events {
        val getCounter = ObserveEvent(prefixes.counter, EventName("get-counter"))
    }
}