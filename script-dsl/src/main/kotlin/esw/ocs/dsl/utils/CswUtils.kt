package esw.ocs.dsl.utils

import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.core.models.Prefix
import csw.params.events.EventKey
import csw.params.events.EventName

interface CswUtils {
    fun eventKey(prefix: String, eventName: String) = EventKey(Prefix(prefix), EventName(eventName))
    fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())
}
