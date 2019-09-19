package esw.ocs.impl.dsl

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.dsl.script.{DiagnosticDsl, EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.impl.core.SequenceOperator
import esw.ocs.impl.internal.SequencerCommandServiceDsl

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    protected val actorSystem: ActorSystem[_],
    private[esw] val locationService: LocationService,
    private[esw] val eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    protected val sequencerAdminFactory: SequencerAdminFactoryApi
) extends LocationServiceDsl
    with SequencerCommandServiceDsl
    with EventServiceDsl
    with TimeServiceDsl
    with DiagnosticDsl
