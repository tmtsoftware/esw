package esw.gateway.server.routes.restless.client

import akka.Done
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import csw.alarm.models.AlarmSeverity
import csw.location.api.codec.DoneCodec
import csw.location.client.HttpCodecs
import csw.location.models.ComponentType
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.GatewayHttpRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.server.routes.restless.messages._
import io.bullet.borer.{Decoder, Encoder}

import scala.async.Async.{async, await}
import scala.concurrent.Future

trait GatewayHttpClient extends GatewayApi with RestlessCodecs with HttpCodecs with DoneCodec {

  import cswContext.actorRuntime.{ec, mat, untypedSystem}

  def post[Req: Encoder, Res: Decoder](req: Req): Future[Res] = async {
    val uri           = Uri("/gateway")
    val requestEntity = await(Marshal(req).to[RequestEntity])
    val request       = HttpRequest(HttpMethods.POST, uri = uri, entity = requestEntity)
    val response      = await(Http().singleRequest(request))
    //todo: make generic status checks and then test if required
    await(Unmarshal(response.entity).to[Res])
  }

  override def setSeverity(
      subsystem: Subsystem,
      componentName: String,
      alarmName: String,
      severity: AlarmSeverity
  ): Future[Either[SetAlarmSeverityFailure, Done]] = {
    post[SetAlarmSeverity, Either[SetAlarmSeverityFailure, Done]](SetAlarmSeverity(subsystem, componentName, alarmName, severity))
  }

  override def process(
      componentType: ComponentType,
      componentName: String,
      command: ControlCommand,
      action: CommandAction
  ): Future[Either[InvalidComponent, CommandResponse]] = {
    post[CommandRequest, Either[InvalidComponent, CommandResponse]](CommandRequest(componentType, componentName, command, action))
  }

  override def publish(event: Event): Future[Done] = {
    post[PublishEvent, Done](PublishEvent(event))
  }

  override def get(eventKeys: Set[EventKey]): Future[Either[EmptyEventKeys, Set[Event]]] = {
    post[GetEvent, Either[EmptyEventKeys, Set[Event]]](GetEvent(eventKeys))
  }

}
