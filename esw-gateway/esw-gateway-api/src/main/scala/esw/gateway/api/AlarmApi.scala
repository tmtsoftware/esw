package esw.gateway.api

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.messages.SetAlarmSeverityFailure

import scala.concurrent.Future

trait AlarmApi {

  def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Either[SetAlarmSeverityFailure, Done]]
}
