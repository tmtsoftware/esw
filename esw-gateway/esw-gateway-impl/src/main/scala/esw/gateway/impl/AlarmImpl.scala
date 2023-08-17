package esw.gateway.impl

import org.apache.pekko.Done
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.AlarmApi
import esw.gateway.api.protocol.SetAlarmSeverityFailure

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
 * Pekko actor client for the Admin service
 *
 * @param alarmService - an instance of AlarmService
 * @param ec - an implicit execution context
 */
class AlarmImpl(alarmService: AlarmService)(implicit ec: ExecutionContext) extends AlarmApi {

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Done] = {
    alarmService.setSeverity(alarmKey, severity).recover { case NonFatal(e) =>
      throw SetAlarmSeverityFailure(e.getMessage)
    }
  }
}
