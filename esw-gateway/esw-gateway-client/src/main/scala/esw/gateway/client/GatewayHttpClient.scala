package esw.gateway.client

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.location.api.codec.DoneCodec
import csw.location.models.ComponentType
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.api.GatewayApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayHttpRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.api.messages._
import msocket.api.HttpClient

import scala.concurrent.Future

abstract class GatewayHttpClient(httpClient: HttpClient) extends GatewayApi with RestlessCodecs with DoneCodec {

  override def setSeverity(
      subsystem: Subsystem,
      componentName: String,
      alarmName: String,
      severity: AlarmSeverity
  ): Future[Either[SetAlarmSeverityFailure, Done]] = {
    httpClient.post[SetAlarmSeverity, Either[SetAlarmSeverityFailure, Done]](
      SetAlarmSeverity(subsystem, componentName, alarmName, severity)
    )
  }

  override def process(
      componentType: ComponentType,
      componentName: String,
      command: ControlCommand,
      action: CommandAction
  ): Future[Either[InvalidComponent, CommandResponse]] = {
    httpClient.post[CommandRequest, Either[InvalidComponent, CommandResponse]](
      CommandRequest(componentType, componentName, command, action)
    )
  }

  override def publish(event: Event): Future[Done] = {
    httpClient.post[PublishEvent, Done](PublishEvent(event))
  }

  override def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]] = {
    httpClient.post[GetEvent, Either[EmptyEventKeys, Set[Event]]](GetEvent(eventKeys))
  }

}
