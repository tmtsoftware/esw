package esw.dsl.script

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.dsl.script.services.javadsl._
import esw.ocs.api.SequencerAdminFactoryApi

class CswServices(
    private[esw] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val actorSystem: ActorSystem[_],
    private[esw] val _locationService: LocationService,
    private[esw] val _eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    protected val sequencerAdminFactory: SequencerAdminFactoryApi
) extends JSequencerCommandServiceDsl
    with JLocationServiceDsl
    with JTimeServiceDsl
    with JEventServiceDsl
    with JCommandServiceDsl
    with JDiagnosticDsl
