package esw.ocs.dsl

import csw.command.client.CommandResponseManager
import esw.ocs.core.SequenceOperator
import esw.ocs.internal.SequencerCommandServiceUtils

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val sequencerCommandService: SequencerCommandServiceUtils
)
//    sequenceId: String,
//    observingMode: String
