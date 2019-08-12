package esw.ocs.dsl

import csw.command.client.CommandResponseManager
import esw.ocs.core.SequenceOperator
import esw.ocs.internal.SequencerCommandServiceUtils
import esw.utils.csw.LocationServiceUtils

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val sequencerCommandService: SequencerCommandServiceUtils,
    val locationServiceUtils: LocationServiceUtils
)
//    sequenceId: String,
//    observingMode: String
