package esw.highlevel.dsl

import akka.actor.typed.ActorSystem
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime

import scala.concurrent.{ExecutionContext, Future}

trait DiagnosticDsl {
  def actorSystem: ActorSystem[_]
  private[esw] def _locationService: LocationService
  private val loc = new LocationServiceUtil(_locationService)(actorSystem)

  def diagnosticMode(
      componentName: String,
      componentType: ComponentType,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): Future[Unit] = {
    loc.resolveComponentRef(componentName, componentType).map(_ ! DiagnosticMode(startTime, hint))
  }

}
