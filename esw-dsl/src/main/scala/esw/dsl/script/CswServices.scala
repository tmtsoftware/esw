package esw.dsl.script

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.dsl.script.services._
import esw.ocs.api.SequencerAdminFactoryApi
import scala.language.experimental.macros

class CswServices(
    private[esw] val sequenceOperatorFactory: () => SequenceOperator,
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
