package esw.dsl.script

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.ocs.api.SequencerAdminFactoryApi

class CswServices(
    private[esw] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val actorSystem: ActorSystem[_],
    private[esw] val locationService: LocationService,
    private[esw] val eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory,
    private[esw] val sequencerAdminFactory: SequencerAdminFactoryApi
)
