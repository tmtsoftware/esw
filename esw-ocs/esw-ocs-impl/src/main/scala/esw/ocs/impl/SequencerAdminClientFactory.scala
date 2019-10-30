package esw.ocs.impl

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
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
    implicit val untypedSystem: actor.ActorSystem = actorSystem.toClassic
    val postClient                                = new HttpPostTransport[SequencerAdminPostRequest](postUrl, encoding, tokenFactory)
    val websocketClient                           = new WebsocketTransport[SequencerAdminWebsocketRequest](websocketUrl, encoding)
    new SequencerAdminClient(postClient, websocketClient)
  }
}
