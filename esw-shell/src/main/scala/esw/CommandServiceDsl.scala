package esw

import _root_.shell.utils.Extensions.FutureExt
import _root_.shell.utils.Timeouts
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.framework.ShellWiring
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerImpl

import scala.concurrent.Future

class CommandServiceDsl(val shellWiring: ShellWiring) {
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = shellWiring.wiring.actorSystem

  import Timeouts.defaultTimeout
  import typedSystem.executionContext
  private val locationUtil: LocationServiceUtil = new LocationServiceUtil(shellWiring.cswContext.locationService)

  def sequencerCommandService(subsystem: Subsystem, observingMode: String): SequencerApi =
    locationUtil
      .resolveSequencer(subsystem, observingMode, Timeouts.defaultDuration)
      .map(e => new SequencerImpl(throwLeft(e).sequencerRef))
      .await()

  def assemblyCommandService(prefix: String): CommandService =
    CommandServiceFactory.make(resolveAkkaLocation(prefix, Assembly).await())

  def hcdCommandService(prefix: String): CommandService =
    CommandServiceFactory.make(resolveAkkaLocation(prefix, HCD).await())

  def resolveAkkaLocation(prefix: String, componentType: ComponentType): Future[AkkaLocation] =
    locationUtil
      .resolve(AkkaConnection(ComponentId(Prefix(prefix), componentType)), Timeouts.resolveLocationDuration)
      .map(throwLeft)

  def throwLeft[T](e: Either[EswLocationError, T]): T =
    e match {
      case Right(t)  => t
      case Left(err) => throw new RuntimeException(err.msg)
    }
}
