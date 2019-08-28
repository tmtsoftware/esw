package esw.ocs.dsl

import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import esw.highlevel.dsl.{EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.core.SequenceOperator
import esw.ocs.internal.SequencerCommandServiceUtils

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val sequencerCommandService: SequencerCommandServiceUtils,
    private[esw] val eventService: EventService,
    private[ocs] val timeServiceDsl: TimeServiceDsl
) extends EventServiceDsl
    with LocationServiceDsl
//    sequenceId: String,
//    observingMode: String
