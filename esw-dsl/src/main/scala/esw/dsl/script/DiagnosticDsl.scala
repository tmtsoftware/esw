package esw.dsl.script

import akka.actor.typed.ActorSystem
import csw.command.client.messages.DiagnosticDataMessage.{DiagnosticMode, OperationsMode}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.dsl.sequence_manager.LocationServiceUtil

import scala.concurrent.{ExecutionContext, Future}

trait DiagnosticDsl {

  private[esw] val locationService: LocationService
  protected implicit val actorSystem: ActorSystem[_]
  private val locationServiceUtil: LocationServiceUtil = new LocationServiceUtil(locationService)

  def diagnosticMode(
      componentName: String,
      componentType: ComponentType,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    locationServiceUtil.resolveComponentRef(componentName, componentType).map(_ ! DiagnosticMode(startTime, hint))

  def operationsMode(
      componentName: String,
      componentType: ComponentType
  )(implicit ec: ExecutionContext): Future[Unit] =
    locationServiceUtil.resolveComponentRef(componentName, componentType).map(_ ! OperationsMode)

}
