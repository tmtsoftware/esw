package esw.ocs.dsl.params

import csw.params.commands.Command
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import esw.ocs.dsl.nullable

val Command.obsId: String?
    get() = jMaybeObsId().map { it.obsId() }.nullable()

fun sequenceOf(vararg sequenceCommand: SequenceCommand) = Sequence.create(sequenceCommand.toList())

