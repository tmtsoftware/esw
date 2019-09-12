package esw.ocs.dsl.params

import csw.params.commands.Command
import esw.ocs.dsl.nullable

val Command.obsId: String?
    get() {
        return jMaybeObsId().map { it.obsId() }.nullable()
    }
