package esw.dsl.script.services.javadsl

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import csw.command.client.SequencerCommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import esw.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

trait JSequencerCommandServiceDsl {

  private[esw] def _locationService: LocationService
  implicit protected def actorSystem: ActorSystem[_]
  private implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  def submitSequence(sequencerName: String, observingMode: String, sequence: Sequence): CompletionStage[SubmitResponse] =
    new LocationServiceUtil(_locationService)
      .findSequencer(sequencerName, observingMode)
      .flatMap(location => SequencerCommandServiceFactory.make(location)(actorSystem).submitAndWait(sequence))
      .toJava
}
