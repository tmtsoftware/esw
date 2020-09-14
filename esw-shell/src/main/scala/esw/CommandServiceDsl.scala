package esw

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.prefix.models.Subsystem
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerImpl
import shell.utils.Extensions.FutureExt

import scala.concurrent.ExecutionContext

class CommandServiceDsl(val locationUtils: LocationUtils)(implicit val actorSystem: ActorSystem[_]) {

  implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  def sequencerCommandService(subsystem: Subsystem, obsModeName: String): SequencerApi =
    locationUtils
      .findSequencer(subsystem, obsModeName)
      .map(new SequencerImpl(_))
      .await()

  def assemblyCommandService(prefix: String): CommandService =
    CommandServiceFactory.make(locationUtils.findAkkaLocation(prefix, Assembly).await())

  def hcdCommandService(prefix: String): CommandService =
    CommandServiceFactory.make(locationUtils.findAkkaLocation(prefix, HCD).await())

}
