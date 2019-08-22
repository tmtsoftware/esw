package esw.gateway.impl

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.params.core.models.Subsystem
import esw.gateway.api.GatewayApi
import esw.gateway.api.messages.SetAlarmSeverityFailure

import scala.concurrent.Future
import scala.util.control.NonFatal

trait AlarmGatewayImpl extends GatewayApi {

  import cswContext._
  import actorRuntime.ec
  override def setSeverity(
      subsystem: Subsystem,
      componentName: String,
      alarmName: String,
      severity: AlarmSeverity
  ): Future[Either[SetAlarmSeverityFailure, Done]] = {
    alarmService
      .setSeverity(AlarmKey(subsystem, componentName, alarmName), severity)
      .map(Right(_))
      .recover {
        case NonFatal(e) => Left(SetAlarmSeverityFailure(e.getMessage))
      }
  }
}
