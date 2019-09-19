package esw.dsl.script.javadsl

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.DiagnosticDataMessage.{DiagnosticMode, OperationsMode}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.{SequencerAdminApi, SequencerAdminFactoryApi}

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

trait JDiagnosticDsl {

  private[esw] val _locationService: LocationService
  protected implicit val actorSystem: ActorSystem[_]
  protected val sequencerAdminFactory: SequencerAdminFactoryApi
  private val locationServiceUtil: LocationServiceUtil = new LocationServiceUtil(_locationService)

  def diagnosticModeForComponent(
      componentName: String,
      componentType: ComponentType,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): CompletionStage[Unit] =
    sendMsgToComponent(componentName, componentType, DiagnosticMode(startTime, hint))

  def operationsModeForComponent(
      componentName: String,
      componentType: ComponentType
  )(implicit ec: ExecutionContext): CompletionStage[Unit] =
    sendMsgToComponent(componentName, componentType, OperationsMode)

  def diagnosticModeForSequencer(
      sequencerId: String,
      observingMode: String,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): CompletionStage[Unit] =
    sendMsgToSequencer(sequencerId, observingMode, (x: SequencerAdminApi) => x.diagnosticMode(startTime, hint))

  def operationsModeForSequencer(
      sequencerId: String,
      observingMode: String
  )(implicit ec: ExecutionContext): CompletionStage[Unit] =
    sendMsgToSequencer(sequencerId, observingMode, (x: SequencerAdminApi) => x.operationsMode())

  private def sendMsgToSequencer(
      sequencerId: String,
      observingMode: String,
      action: SequencerAdminApi => Unit
  )(implicit ec: ExecutionContext): CompletionStage[Unit] =
    sequencerAdminFactory.make(sequencerId, observingMode).map(action).toJava

  private def sendMsgToComponent(
      componentName: String,
      componentType: ComponentType,
      msg: DiagnosticDataMessage
  )(implicit ec: ExecutionContext): CompletionStage[Unit] =
    locationServiceUtil
      .resolveComponentRef(componentName, componentType)
      .map(x => x ! msg)
      .toJava
}
