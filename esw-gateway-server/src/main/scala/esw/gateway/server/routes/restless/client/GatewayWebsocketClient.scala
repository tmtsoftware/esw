package esw.gateway.server.routes.restless.client

import akka.stream.scaladsl.Source
import csw.location.models.ComponentType
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.models.{Id, Subsystem}
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventKey}
import esw.gateway.server.routes.restless.api.GatewayApi
import esw.gateway.server.routes.restless.codecs.RestlessCodecs
import esw.gateway.server.routes.restless.messages.GatewayWebsocketRequest.{
  QueryFinal,
  Subscribe,
  SubscribeCurrentState,
  SubscribeWithPattern
}
import esw.gateway.server.routes.restless.messages._
import msocket.core.client.ClientSocket

import scala.concurrent.Future

trait GatewayWebsocketClient extends GatewayApi with RestlessCodecs {
  def socket: ClientSocket[GatewayWebsocketRequest]

  import cswCtx.actorRuntime.mat

  override def queryFinal(
      componentType: ComponentType,
      componentName: String,
      runId: Id
  ): Future[Either[InvalidComponent, SubmitResponse]] = {
    socket.requestResponse[Either[InvalidComponent, SubmitResponse]](QueryFinal(componentType, componentName, runId))
  }

  override def subscribeCurrentState(
      componentType: ComponentType,
      componentName: String,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ): Source[CurrentState, Future[Option[CommandError]]] = {
    socket.requestStreamWithError[CurrentState, CommandError](
      SubscribeCurrentState(componentType, componentName, stateNames, maxFrequency)
    )
  }

  override def subscribe(eventKeys: Set[EventKey], maxFrequency: Option[Int]): Source[Event, Future[Option[EventError]]] = {
    socket.requestStreamWithError[Event, EventError](Subscribe(eventKeys, maxFrequency))
  }

  override def pSubscribe(
      subsystem: Subsystem,
      maxFrequency: Option[Int],
      pattern: String
  ): Source[Event, Future[Option[InvalidMaxFrequency]]] = {
    socket.requestStreamWithError[Event, InvalidMaxFrequency](SubscribeWithPattern(subsystem, maxFrequency, pattern))
  }
}
