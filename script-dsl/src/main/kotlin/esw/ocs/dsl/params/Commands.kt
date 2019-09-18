package esw.ocs.dsl.params

import csw.params.commands.Command
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.dsl.nullable

val Command.obsId: String?
    get() = jMaybeObsId().map { it.obsId() }.nullable()

val Command.runId: Id
    get() = runId()

fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())
