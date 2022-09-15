/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.api.clients

import akka.actor.typed.ActorSystem
import csw.command.api.client.CommandServiceClient
import csw.command.api.scaladsl.CommandService
import csw.location.api.models.ComponentId
import esw.gateway.api.protocol.{GatewayRequest, GatewayStreamRequest}
import esw.ocs.api.SequencerApi
import esw.ocs.api.client.SequencerClient
import msocket.api.Transport

/**
 * A client factory for creating HTTP clients for components & sequencers.
 * @param postTransport - An HTTP Transport class for HTTP calls for the components & sequencers.
 * @param websocketTransport - An Web socket Transport class for the components & sequencers.
 * @param actorSystem - An implicit actor system.
 */
class ClientFactory(postTransport: Transport[GatewayRequest], websocketTransport: Transport[GatewayStreamRequest])(implicit
    actorSystem: ActorSystem[_]
) {

  import esw.gateway.api.codecs.GatewayCodecs._
  import actorSystem.executionContext

  def component(componentId: ComponentId): CommandService =
    new CommandServiceClient(
      postTransport.contraMap(GatewayRequest.ComponentCommand(componentId, _)),
      websocketTransport.contraMap(GatewayStreamRequest.ComponentCommand(componentId, _))
    )

  def sequencer(componentId: ComponentId): SequencerApi =
    new SequencerClient(
      postTransport.contraMap(GatewayRequest.SequencerCommand(componentId, _)),
      websocketTransport.contraMap(GatewayStreamRequest.SequencerCommand(componentId, _))
    )

}
