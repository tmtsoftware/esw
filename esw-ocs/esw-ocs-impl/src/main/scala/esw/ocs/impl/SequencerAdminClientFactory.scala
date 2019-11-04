package esw.ocs.impl

import akka.actor.typed.ActorSystem
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerAdminWebsocketRequest}
import msocket.impl.Encoding
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

object SequencerAdminClientFactory extends SequencerAdminHttpCodecs {
  def make(postUrl: String, websocketUrl: String, encoding: Encoding[_], tokenFactory: () => Option[String])(
      implicit actorSystem: ActorSystem[_]
  ): SequencerAdminClient = {
    val postClient      = new HttpPostTransport[SequencerAdminPostRequest](postUrl, encoding, tokenFactory)
    val websocketClient = new WebsocketTransport[SequencerAdminWebsocketRequest](websocketUrl, encoding)
    new SequencerAdminClient(postClient, websocketClient)
  }
}
