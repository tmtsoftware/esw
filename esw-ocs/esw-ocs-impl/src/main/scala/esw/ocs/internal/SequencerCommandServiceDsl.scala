package esw.ocs.internal

import akka.actor.typed.ActorSystem
import csw.command.client.SequencerCommandServiceFactory
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence

import scala.concurrent.Future

trait SequencerCommandServiceDsl {
  private[esw] def actorSystem: ActorSystem[_]
  def submitSequence(location: AkkaLocation, sequence: Sequence): Future[SubmitResponse] = {
    SequencerCommandServiceFactory.make(location)(actorSystem).submit(sequence)
  }
}
