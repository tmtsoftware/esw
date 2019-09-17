package esw.highlevel.dsl

import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime

import scala.concurrent.{ExecutionContext, Future}

trait DiagnosticDsl { self: LocationServiceDsl =>

  def diagnosticMode(
      componentName: String,
      componentType: ComponentType,
      startTime: UTCTime,
      hint: String
  )(implicit ec: ExecutionContext): Future[Unit] =
    resolveComponentRef(componentName, componentType).map(_ ! DiagnosticMode(startTime, hint))

}
