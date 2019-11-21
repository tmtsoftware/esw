package esw.gateway.api.clients

import akka.stream.scaladsl.Source
import csw.location.models.ComponentId
import csw.params.commands.CommandResponse.{OnewayResponse, SubmitResponse, ValidateResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.CommandApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest.{Oneway, Submit, Validate}
import esw.gateway.api.protocol.WebsocketRequest.{QueryFinal, SubscribeCurrentState}
import esw.gateway.api.protocol.{InvalidComponent, PostRequest, WebsocketRequest}
import msocket.api.Transport
import msocket.api.models.Subscription

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class CommandClient(postClient: Transport[PostRequest], websocketClient: Transport[WebsocketRequest])
    extends CommandApi
    with GatewayCodecs {

  override def submit(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, SubmitResponse]] = {
    postClient.requestResponse[Either[InvalidComponent, SubmitResponse]](
      Submit(componentId, command)
    )
  }

  override def oneway(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, OnewayResponse]] = {
    postClient.requestResponse[Either[InvalidComponent, OnewayResponse]](
      Oneway(componentId, command)
    )
  }

  override def validate(componentId: ComponentId, command: ControlCommand): Future[Either[InvalidComponent, ValidateResponse]] = {
    postClient.requestResponse[Either[InvalidComponent, ValidateResponse]](
      Validate(componentId, command)
    )
  }

  override def queryFinal(componentId: ComponentId, runId: Id): Future[Either[InvalidComponent, SubmitResponse]] = {
    websocketClient.requestResponse[Either[InvalidComponent, SubmitResponse]](QueryFinal(componentId, runId), 1.hours)
  }

  override def subscribeCurrentState(
      componentId: ComponentId,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ): Source[CurrentState, Subscription] = {
    websocketClient.requestStream[CurrentState](
      SubscribeCurrentState(componentId, stateNames, maxFrequency)
    )
  }
}
