package esw.gateway.api

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.params.core.models.Subsystem
import esw.gateway.api.messages.SetAlarmSeverityFailure

import scala.concurrent.Future

trait AlarmServiceApi {

  def setSeverity(
      subsystem: Subsystem,
      componentName: String,
      alarmName: String,
      severity: AlarmSeverity
  ): Future[Either[SetAlarmSeverityFailure, Done]]
}
