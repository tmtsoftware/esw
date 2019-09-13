package esw.ocs.impl.dsl

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import csw.command.client.CommandResponseManager
import csw.event.api.scaladsl.EventService
import csw.location.api.scaladsl.LocationService
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.highlevel.dsl.javadsl.{JCommandServiceDsl, JEventServiceDsl, JLocationServiceDsl, JTimeServiceDsl}
import esw.highlevel.dsl.{EventServiceDsl, LocationServiceDsl, TimeServiceDsl}
import esw.ocs.impl.core.SequenceOperator
import esw.ocs.impl.internal.SequencerCommandServiceDsl

import scala.compat.java8.FutureConverters.FutureOps

class CswServices(
    private[ocs] val sequenceOperatorFactory: () => SequenceOperator,
    val crm: CommandResponseManager,
    private[esw] val actorSystem: ActorSystem[_],
    private[esw] val locationService: LocationService,
    private[esw] val eventService: EventService,
    private[esw] val timeServiceSchedulerFactory: TimeServiceSchedulerFactory
) extends SequencerCommandServiceDsl
    with LocationServiceDsl
    with JLocationServiceDsl
    with EventServiceDsl
    with JEventServiceDsl
    with TimeServiceDsl
    with JTimeServiceDsl
    with JCommandServiceDsl {

  // fixme: move it to appropriate place
  def jSubmitSequence(sequencerId: String, observingMode: String, sequence: Sequence): CompletionStage[SubmitResponse] =
    resolveSequencer(sequencerId, observingMode).flatMap { loc =>
      super.submitSequence(loc, sequence)
    }.toJava

}
