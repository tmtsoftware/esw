package esw.gateway.api.clients

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import esw.gateway.api.AlarmApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.SetAlarmSeverity
import msocket.api.Transport

import scala.concurrent.Future

class AlarmClient(postClient: Transport[GatewayRequest]) extends AlarmApi with GatewayCodecs {

  override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Done] = {
    postClient.requestResponse[Done](SetAlarmSeverity(alarmKey, severity))
  }
}
