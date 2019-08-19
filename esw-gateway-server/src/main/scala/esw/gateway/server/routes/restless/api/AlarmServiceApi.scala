package esw.gateway.server.routes.restless.api

import akka.Done
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg
import esw.gateway.server.routes.restless.messages.HttpRequestMsg.SetAlarmSeverityMsg

import scala.concurrent.Future

trait AlarmServiceApi {
  def setSeverity(setAlarmSeverityMsg: SetAlarmSeverityMsg): Future[Either[ErrorResponseMsg, Done]]
}
