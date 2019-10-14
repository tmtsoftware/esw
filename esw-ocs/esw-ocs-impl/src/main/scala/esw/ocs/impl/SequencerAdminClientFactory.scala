package esw.ocs.impl

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerAdminWebsocketRequest}
import mscoket.impl.post.HttpPostTransport
import mscoket.impl.ws.WebsocketTransport

object SequencerAdminClientFactory extends SequencerAdminHttpCodecs {
  def make(postUrl: String, websocketUrl: String, tokenFactory: => Option[String])(
      implicit actorSystem: ActorSystem[_]
  ): SequencerAdminClient = {
    implicit val untypedSystem: actor.ActorSystem = actorSystem.toClassic
    val postClient                                = new HttpPostTransport[SequencerAdminPostRequest](postUrl, None)
    val websocketClient                           = new WebsocketTransport[SequencerAdminWebsocketRequest](websocketUrl)
    new SequencerAdminClient(postClient, websocketClient)
  }
}
