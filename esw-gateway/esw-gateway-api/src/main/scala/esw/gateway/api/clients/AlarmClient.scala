package esw.gateway.api.clients

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.AlarmServiceApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest.SetAlarmSeverity
import esw.gateway.api.messages.SetAlarmSeverityFailure
import msocket.api.PostClient

import scala.concurrent.Future

class AlarmClient(postClient: PostClient) extends AlarmServiceApi with RestlessCodecs {

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Either[SetAlarmSeverityFailure, Done]] = {
    postClient.requestResponse[SetAlarmSeverity, Either[SetAlarmSeverityFailure, Done]](
      SetAlarmSeverity(alarmKey, severity)
    )
  }
}
