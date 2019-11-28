package esw.ocs.impl

import akka.actor.typed.ActorSystem
import esw.ocs.api.client.SequencerClient
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.{SequencerPostRequest, SequencerWebsocketRequest}
import msocket.impl.Encoding
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

object SequencerClientFactory extends SequencerHttpCodecs {
  def make(postUrl: String, websocketUrl: String, encoding: Encoding[_], tokenFactory: () => Option[String])(
      implicit actorSystem: ActorSystem[_]
  ): SequencerClient = {
    import actorSystem.executionContext

    val postClient      = new HttpPostTransport[SequencerPostRequest](postUrl, encoding, tokenFactory)
    val websocketClient = new WebsocketTransport[SequencerWebsocketRequest](websocketUrl, encoding)
    new SequencerClient(postClient, websocketClient)
  }
}
