package esw.gateway.api.clients

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.AlarmApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest.SetAlarmSeverity
import esw.gateway.api.messages.{PostRequest, SetAlarmSeverityFailure}

import scala.concurrent.Future

class AlarmClient(postClient: RequestClient[PostRequest]) extends AlarmApi with RestlessCodecs {

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Either[SetAlarmSeverityFailure, Done]] = {
    postClient.requestResponse[Either[SetAlarmSeverityFailure, Done]](
      SetAlarmSeverity(alarmKey, severity)
    )
  }
}
