package esw.ocs.impl

import akka.actor.typed.ActorSystem
import esw.ocs.api.client.SequencerCommandClient
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest
import msocket.impl.Encoding
import msocket.impl.ws.WebsocketTransport

object SequencerCommandClientFactory extends SequencerHttpCodecs {
  def make(websocketUrl: String, encoding: Encoding[_], tokenFactory: () => Option[String])(
      implicit actorSystem: ActorSystem[_]
  ): SequencerCommandClient = {
    val websocketClient = new WebsocketTransport[SequencerCommandWebsocketRequest](websocketUrl, encoding)
    new SequencerCommandClient(websocketClient)
  }
}
