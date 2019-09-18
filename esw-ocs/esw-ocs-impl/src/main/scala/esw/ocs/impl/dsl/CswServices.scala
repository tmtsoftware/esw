package esw.ocs.impl.dsl

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.dsl.{DiagnosticDsl, EventServiceDsl, TimeServiceDsl}
import esw.ocs.impl.core.SequenceOperator
import esw.ocs.impl.internal.SequencerCommandServiceDsl
import esw.sequence_manager.LocationServiceUtil

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    override protected val actorSystem: ActorSystem[_],
    override private[esw] val locationService: LocationService,
    private[esw] val eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory
) extends LocationServiceUtil(locationService)(actorSystem)
    with SequencerCommandServiceDsl
    with EventServiceDsl
    with TimeServiceDsl
    with DiagnosticDsl
