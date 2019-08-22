package esw.gateway.impl

import akka.Done
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.AlarmServiceApi
import esw.gateway.api.messages.SetAlarmSeverityFailure

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AlarmServiceImpl(alarmService: AlarmService)(implicit ec: ExecutionContext) extends AlarmServiceApi {

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Either[SetAlarmSeverityFailure, Done]] = {
    alarmService
      .setSeverity(alarmKey, severity)
      .map(Right(_))
      .recover {
        case NonFatal(e) => Left(SetAlarmSeverityFailure(e.getMessage))
      }
  }
}
