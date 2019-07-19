package esw.ocs.dsl

import csw.command.client.CommandResponseManager
import esw.ocs.core.SequenceOperator

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager
)
//    sequenceId: String,
//    observingMode: String
