package esw.ocs.dsl

import csw.command.client.CommandResponseManager
import esw.highlevel.dsl.{EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.core.SequenceOperator
import esw.ocs.internal.SequencerCommandServiceUtils

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val sequencerCommandService: SequencerCommandServiceUtils,
    private[ocs] val locationServiceDsl: LocationServiceDsl,
    private[ocs] val eventServiceDsl: EventServiceDsl,
    private[ocs] val timeServiceDsl: TimeServiceDsl
)
//    sequenceId: String,
//    observingMode: String
