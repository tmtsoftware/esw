package esw.dsl.script.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.typed.ActorSystem
import csw.command.client.messages.DiagnosticDataMessage
import csw.command.client.messages.DiagnosticDataMessage.{DiagnosticMode, OperationsMode}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.dsl.sequence_manager.LocationServiceUtil

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

trait JDiagnosticDsl {

  private[esw] val _locationService: LocationService
  protected implicit val actorSystem: ActorSystem[_]
  private val locationServiceUtil: LocationServiceUtil = new LocationServiceUtil(_locationService)

  def diagnosticMode(
      componentName: String,
      componentType: ComponentType,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): CompletableFuture[Unit] =
    sendMsg(componentName, componentType, DiagnosticMode(startTime, hint))

  def operationsMode(
      componentName: String,
      componentType: ComponentType
  )(implicit ec: ExecutionContext): CompletableFuture[Unit] =
    sendMsg(componentName, componentType, OperationsMode)

  private def sendMsg(
      componentName: String,
      componentType: ComponentType,
      msg: DiagnosticDataMessage
  )(implicit ec: ExecutionContext): CompletableFuture[Unit] = {
    locationServiceUtil
      .resolveComponentRef(componentName, componentType)
      .map(x => x ! msg)
      .toJava
      .toCompletableFuture
  }
}
