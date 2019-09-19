package esw.dsl.script

import akka.actor.typed.ActorSystem
import csw.command.client.messages.DiagnosticDataMessage.{DiagnosticMode, OperationsMode}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.SequencerAdminFactoryApi

import scala.concurrent.{ExecutionContext, Future}

// fixme: implicit ec in script is strand ec, do we need that here?
//  can we use actor systems ec here and remove implicit ec from each api?
trait DiagnosticDsl {
  protected val sequencerAdminFactory: SequencerAdminFactoryApi
  implicit protected val actorSystem: ActorSystem[_]
  private[esw] val locationService: LocationService
  private val locationServiceUtil = new LocationServiceUtil(locationService)

  def diagnosticModeForComponent(
      componentName: String,
      componentType: ComponentType,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    locationServiceUtil.resolveComponentRef(componentName, componentType).map(_ ! DiagnosticMode(startTime, hint))

  def operationsModeForComponent(
      componentName: String,
      componentType: ComponentType
  )(implicit ec: ExecutionContext): Future[Unit] =
    locationServiceUtil.resolveComponentRef(componentName, componentType).map(_ ! OperationsMode)

  def diagnosticModeForSequencer(
      sequencerId: String,
      observingMode: String,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    sequencerAdminFactory.make(sequencerId, observingMode).flatMap(_.diagnosticMode(startTime, hint)).map(_ => ())

  def operationsModeForSequencer(
      sequencerId: String,
      observingMode: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    sequencerAdminFactory.make(sequencerId, observingMode).flatMap(_.operationsMode()).map(_ => ())

}
