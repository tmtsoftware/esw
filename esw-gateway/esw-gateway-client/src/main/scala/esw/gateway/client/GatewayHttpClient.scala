package esw.gateway.client

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
import esw.gateway.api.GatewayApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayHttpRequest.{CommandRequest, GetEvent, PublishEvent, SetAlarmSeverity}
import esw.gateway.api.messages._
import io.bullet.borer.{Decoder, Encoder}
import msocket.core.api.EitherCodecs

import scala.async.Async.{async, await}
import scala.concurrent.Future

trait GatewayHttpClient extends GatewayApi with RestlessCodecs with HttpCodecs with DoneCodec with EitherCodecs {

  import cswContext.actorRuntime.{ec, mat, untypedSystem}

  def serverIp: String
  def serverPort: String

  def post[Req: Encoder, Res: Decoder](req: Req): Future[Res] = async {
    val baseUri       = s"http://$serverIp:$serverPort/gateway"
    val uri           = Uri(baseUri)
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
