package esw.sm.api.actor.client

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import csw.location.api.models.{AkkaLocation, HttpLocation, Location, TcpLocation}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.client.SequenceManagerClient
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerPostRequest
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport

object SequenceManagerApiFactory {

  def make(location: Location)(implicit actorSystem: ActorSystem[_]): SequenceManagerApi = {
    location match {
      case _: TcpLocation =>
        throw new RuntimeException("Only AkkaLocation and HttpLocation can be used to access SequenceManager")
      case akkaLoc: AkkaLocation => new SequenceManagerImpl(akkaLoc)
      case httpLoc: HttpLocation => httpClient(httpLoc)
    }
  }

  private def httpClient(httpLocation: HttpLocation)(implicit actorSystem: ActorSystem[_]): SequenceManagerClient = {
    import SequenceManagerHttpCodec._

    val baseUri    = httpLocation.uri.toString
    val postUri    = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val postClient = new HttpPostTransport[SequenceManagerPostRequest](postUri, ContentType.Json, () => None)
    new SequenceManagerClient(postClient)
  }
}
