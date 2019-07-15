package esw.ocs.framework.dsl

import csw.command.client.CommandResponseManager
import esw.ocs.framework.core.SequenceOperator

class CswServices(
    private[framework] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager
)
//    sequenceId: String,
//    observingMode: String
