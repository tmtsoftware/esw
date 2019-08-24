package esw.gateway.server2

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import csw.location.client.HttpCodecs
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest
import esw.gateway.api.messages.PostRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.api.{AlarmServiceApi, CommandServiceApi, EventServiceApi}
import msocket.api.PostHandler

class PostHandlerImpl(alarmServiceApi: AlarmServiceApi, commandServiceApi: CommandServiceApi, eventServiceApi: EventServiceApi)()
    extends PostHandler[PostRequest, StandardRoute]
    with RestlessCodecs
    with HttpCodecs {

  override def handle(request: PostRequest): StandardRoute = request match {
    case CommandRequest(componentId, command, action) =>
      complete(commandServiceApi.process(componentId, command, action))

    case PublishEvent(event) => complete(eventServiceApi.publish(event))
    case GetEvent(eventKeys) => complete(eventServiceApi.get(eventKeys))

    case SetAlarmSeverity(alarmKey, severity) =>
      complete(alarmServiceApi.setSeverity(alarmKey, severity))
  }
}
