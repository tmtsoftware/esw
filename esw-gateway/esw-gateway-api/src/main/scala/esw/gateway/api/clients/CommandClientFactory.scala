package esw.gateway.api.clients

import akka.actor.typed.ActorSystem
import csw.command.api.client.CommandServiceClient
import csw.command.api.scaladsl.CommandService
import csw.location.models.ComponentId
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import msocket.api.Transport

class CommandClientFactory(postTransport: Transport[PostRequest], websocketTransport: Transport[WebsocketRequest])(
    implicit actorSystem: ActorSystem[_]
) {

  import esw.gateway.api.codecs.GatewayCodecs._

  def make(componentId: ComponentId): CommandService = new CommandServiceClient(
    postTransport.contraMap(PostRequest.ComponentCommand(componentId, _)),
    websocketTransport.contraMap(WebsocketRequest.ComponentCommand(componentId, _))
  )
}
