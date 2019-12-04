package esw.gateway.impl

import akka.Done
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.AlarmApi
import esw.gateway.api.protocol.SetAlarmSeverityFailure

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AlarmImpl(alarmService: AlarmService)(implicit ec: ExecutionContext) extends AlarmApi {

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Done] = {
    alarmService.setSeverity(alarmKey, severity).recover {
      case NonFatal(e) => throw SetAlarmSeverityFailure(e.getMessage)
    }
  }
}
