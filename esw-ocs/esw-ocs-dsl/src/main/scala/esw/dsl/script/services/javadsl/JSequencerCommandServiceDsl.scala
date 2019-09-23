package esw.dsl.script.services.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.typed.ActorSystem
import csw.command.client.SequencerCommandServiceFactory
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence

import scala.compat.java8.FutureConverters.FutureOps

trait JSequencerCommandServiceDsl {

  protected def actorSystem: ActorSystem[_]

  def submitSequence(location: AkkaLocation, sequence: Sequence): CompletableFuture[SubmitResponse] =
    SequencerCommandServiceFactory.make(location)(actorSystem).submitAndWait(sequence).toJava.toCompletableFuture
}
