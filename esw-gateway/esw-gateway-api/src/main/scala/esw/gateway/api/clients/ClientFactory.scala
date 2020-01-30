package esw.gateway.api.clients

import akka.actor.typed.ActorSystem
import csw.command.api.client.CommandServiceClient
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentId
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.ocs.api.SequencerApi
import esw.ocs.api.client.SequencerClient
import msocket.api.Transport

class ClientFactory(postTransport: Transport[PostRequest], websocketTransport: Transport[WebsocketRequest])(
    implicit actorSystem: ActorSystem[_]
) {

  import esw.gateway.api.codecs.GatewayCodecs._
  import actorSystem.executionContext

  def component(componentId: ComponentId): CommandService = new CommandServiceClient(
    postTransport.contraMap(PostRequest.ComponentCommand(componentId, _)),
    websocketTransport.contraMap(WebsocketRequest.ComponentCommand(componentId, _))
  )

  def sequencer(componentId: ComponentId): SequencerApi = new SequencerClient(
    postTransport.contraMap(PostRequest.SequencerCommand(componentId, _)),
    websocketTransport.contraMap(WebsocketRequest.SequencerCommand(componentId, _))
  )

}
