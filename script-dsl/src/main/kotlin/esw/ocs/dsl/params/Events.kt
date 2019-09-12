package esw.ocs.dsl.params

import csw.params.core.models.Prefix
import csw.params.events.EventKey
import csw.params.events.EventName

fun eventKey(prefix: String, eventName: String) = EventKey(Prefix(prefix), EventName(eventName))