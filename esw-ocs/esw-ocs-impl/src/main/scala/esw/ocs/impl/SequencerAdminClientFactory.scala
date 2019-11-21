package esw.ocs.impl

import akka.actor.typed.ActorSystem
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.{SequencerPostRequest, SequencerWebsocketRequest}
import msocket.impl.Encoding
import msocket.impl.post.HttpPostTransport

object SequencerAdminClientFactory extends SequencerHttpCodecs {
  def make(postUrl: String, wsUrl: String, encoding: Encoding[_], tokenFactory: () => Option[String])(
      implicit actorSystem: ActorSystem[_]
  ): SequencerAdminClient = {
    val postClient      = new HttpPostTransport[SequencerPostRequest](postUrl, encoding, tokenFactory)
    val websocketClient = new HttpPostTransport[SequencerWebsocketRequest](wsUrl, encoding, tokenFactory)
    new SequencerAdminClient(postClient, websocketClient)
  }
}
