package esw.ocs.dsl

import csw.command.client.CommandResponseManager
import esw.ocs.core.SequenceOperator
import esw.ocs.internal.SequencerCommandServiceUtil

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val sequencerCommandService: SequencerCommandServiceUtil
)
//    sequenceId: String,
//    observingMode: String
