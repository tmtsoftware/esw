package esw.ocs.impl.dsl

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.highlevel.dsl.javadsl.{JCommandServiceDsl, JEventServiceDsl, JLocationServiceDsl, JTimeServiceDsl}
import esw.highlevel.dsl.{DiagnosticDsl, EventServiceDsl}
import esw.ocs.impl.core.SequenceOperator
import esw.ocs.impl.internal.SequencerCommandServiceDsl

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    val actorSystem: ActorSystem[_],
    private[esw] val _locationService: LocationService,
    private[esw] val eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory
) extends SequencerCommandServiceDsl
    with JLocationServiceDsl
    with JTimeServiceDsl
    with EventServiceDsl
    with JEventServiceDsl
    with JCommandServiceDsl
    with DiagnosticDsl {

  // fixme: move it to appropriate place
//  def jSubmitSequence(sequencerId: String, observingMode: String, sequence: Sequence): CompletionStage[SubmitResponse] =
//    resolveSequencer(sequencerId, observingMode).flatMap { loc =>
//      super.submitSequence(loc, sequence)
//    }.toJava

}
