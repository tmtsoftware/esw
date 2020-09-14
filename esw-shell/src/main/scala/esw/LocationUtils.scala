package esw

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}

import scala.concurrent.{ExecutionContext, Future}

class LocationUtils(locationService: LocationService)(implicit
    val actorSystem: ActorSystem[_]
) {
  implicit lazy val ec: ExecutionContext     = actorSystem.executionContext
  lazy val locationUtil: LocationServiceUtil = new LocationServiceUtil(locationService)

  def findSequencer(subsystem: Subsystem, obsModeName: String): Future[ActorRef[SequencerMsg]] = {
    locationUtil
      .findSequencer(subsystem, obsModeName)
      .map(throwLeft(_).sequencerRef)
  }

  def findAkkaLocation(prefix: String, componentType: ComponentType): Future[AkkaLocation] =
    locationUtil
      .find(AkkaConnection(ComponentId(Prefix(prefix), componentType)))
      .map(throwLeft)

  def throwLeft[T](e: Either[EswLocationError, T]): T =
    e match {
      case Right(t)  => t
      case Left(err) => throw new RuntimeException(err.msg)
    }
}
