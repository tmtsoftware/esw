package esw.ocs.internal

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.client.SequencerCommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence

import scala.concurrent.Future

class SequencerCommandServiceUtils(locationService: LocationService)(implicit system: ActorSystem[_], timeout: Timeout) {
  import system.executionContext
  def submitSequence(sequencerName: String, sequence: Sequence): Future[SubmitResponse] = {
    val connection = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))

    locationService.resolve(connection, timeout.duration).flatMap {
      case Some(location) => SequencerCommandServiceFactory.make(location).submit(sequence)
      case None           => throw new IllegalArgumentException(s"Could not find any sequencer with name: $sequencerName")
    }
  }
}
