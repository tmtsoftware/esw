package esw.backend.testkit.stubs

import org.apache.pekko.Done
import csw.alarm.models.{AlarmSeverity, Key}
import esw.gateway.api.AlarmApi

import scala.concurrent.Future

class AlarmStubImpl extends AlarmApi {
  override def setSeverity(alarmKey: Key.AlarmKey, severity: AlarmSeverity): Future[Done] = Future.successful(Done)
}
