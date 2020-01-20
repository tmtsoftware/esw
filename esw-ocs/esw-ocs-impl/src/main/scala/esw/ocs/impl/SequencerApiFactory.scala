package esw.ocs.impl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.location.models.{AkkaLocation, HttpLocation, Location, TcpLocation}
import esw.ocs.api.SequencerApi
import esw.ocs.api.client.SequencerClient
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.{SequencerPostRequest, SequencerWebsocketRequest}
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

import scala.concurrent.duration.DurationLong

object SequencerApiFactory extends SequencerHttpCodecs {

  def make(componentLocation: Location)(implicit actorSystem: ActorSystem[_]): SequencerApi = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    componentLocation match {
      case _: TcpLocation             => throw new RuntimeException("Only AkkaLocation and HttpLocation can be used to access sequencer")
      case akkaLocation: AkkaLocation => new SequencerActorProxy(akkaLocation.sequencerRef)
      case httpLocation: HttpLocation => httpClient(httpLocation)
    }
  }

  private def httpClient(httpLocation: HttpLocation)(implicit actorSystem: ActorSystem[_]): SequencerClient = {
    import actorSystem.executionContext

    val baseUri         = httpLocation.uri.toString
    val postUri         = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val webSocketUri    = Uri(baseUri).withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val postClient      = new HttpPostTransport[SequencerPostRequest](postUri, ContentType.Json, () => None)
    val websocketClient = new WebsocketTransport[SequencerWebsocketRequest](webSocketUri, ContentType.Json)
    new SequencerClient(postClient, websocketClient)
  }
}
