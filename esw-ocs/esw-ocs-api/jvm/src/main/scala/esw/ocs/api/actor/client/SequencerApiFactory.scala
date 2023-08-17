package esw.ocs.api.actor.client

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.location.api.models.{PekkoLocation, HttpLocation, Location, TcpLocation}
import esw.ocs.api.SequencerApi
import esw.ocs.api.client.SequencerClient
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.protocol.{SequencerRequest, SequencerStreamRequest}
import msocket.api.ContentType
import msocket.http.post.HttpPostTransport
import msocket.http.ws.WebsocketTransport

/**
 * This a factory to create instances of sequencer's actor and http client
 */
object SequencerApiFactory extends SequencerServiceCodecs {

  /**
   * This method of the factory takes the Location and returns the appropriate factory
   * means if the Location is an PekkoLocation then an pekkoClient is returned which talks to the sequencer via actor messages
   * and if the Location is a HttpLocation then a HttpClient is returned which communicates with the sequencer via Http Protocols
   * If none of the above type of locations are there then an Exception is returned
   *
   * @param componentLocation - Location of the sequencer
   * @param actorSystem - actorSystem
   * @return a [[esw.ocs.api.SequencerApi]]
   */
  def make(componentLocation: Location)(implicit actorSystem: ActorSystem[_]): SequencerApi =
    componentLocation match {
      case _: TcpLocation => throw new RuntimeException("Only PekkoLocation and HttpLocation can be used to access sequencer")
      case pekkoLocation: PekkoLocation => new SequencerImpl(pekkoLocation.sequencerRef)
      case httpLocation: HttpLocation   => httpClient(httpLocation)
    }

  /*
   * This method is for creating an http client for the sequencer
   */
  private def httpClient(httpLocation: HttpLocation)(implicit actorSystem: ActorSystem[_]): SequencerClient = {
    import actorSystem.executionContext

    val baseUri         = httpLocation.uri.toString
    val postUri         = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val webSocketUri    = Uri(baseUri).withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val postClient      = new HttpPostTransport[SequencerRequest](postUri, ContentType.Json, () => None)
    val websocketClient = new WebsocketTransport[SequencerStreamRequest](webSocketUri, ContentType.Json)
    new SequencerClient(postClient, websocketClient)
  }
}
