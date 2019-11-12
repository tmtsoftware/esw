package esw.ocs.impl

import akka.actor.typed.ActorSystem
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerAdminPostRequest
import msocket.impl.Encoding
import msocket.impl.post.HttpPostTransport

object SequencerAdminClientFactory extends SequencerHttpCodecs {
  def make(postUrl: String, encoding: Encoding[_], tokenFactory: () => Option[String])(
      implicit actorSystem: ActorSystem[_]
  ): SequencerAdminClient = {
    val postClient = new HttpPostTransport[SequencerAdminPostRequest](postUrl, encoding, tokenFactory)
    new SequencerAdminClient(postClient)
  }
}
