package esw.gateway.api.clients

import akka.stream.scaladsl.Source
import csw.location.models.ComponentId
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import esw.gateway.api.CommandApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.PostRequest.CommandRequest
import esw.gateway.api.messages.WebsocketRequest.{QueryFinal, SubscribeCurrentState}
import esw.gateway.api.messages.{CommandAction, CommandError, InvalidComponent, PostRequest, WebsocketRequest}
import msocket.api.{PostClient, WebsocketClient}

import scala.concurrent.Future

class CommandClient(postClient: PostClient[PostRequest], websocketClient: WebsocketClient[WebsocketRequest])
    extends CommandApi
    with RestlessCodecs {

  override def process(
      componentId: ComponentId,
      command: ControlCommand,
      action: CommandAction
  ): Future[Either[InvalidComponent, CommandResponse]] = {
    postClient.requestResponse[Either[InvalidComponent, CommandResponse]](
      CommandRequest(componentId, command, action)
    )
  }

  override def queryFinal(componentId: ComponentId, runId: Id): Future[Either[InvalidComponent, SubmitResponse]] = {
    websocketClient.requestResponse[Either[InvalidComponent, SubmitResponse]](QueryFinal(componentId, runId))
  }

  override def subscribeCurrentState(
      componentId: ComponentId,
      stateNames: Set[StateName],
      maxFrequency: Option[Int]
  ): Source[CurrentState, Future[Option[CommandError]]] = {
    websocketClient.requestStreamWithError[CurrentState, CommandError](
      SubscribeCurrentState(componentId, stateNames, maxFrequency)
    )
  }
}
