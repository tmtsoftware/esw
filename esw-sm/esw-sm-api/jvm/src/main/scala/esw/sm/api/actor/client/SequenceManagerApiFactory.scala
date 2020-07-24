package esw.sm.api.actor.client

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import csw.location.api.models.{AkkaLocation, HttpLocation}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.client.SequenceManagerClient
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerPostRequest
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport

object SequenceManagerApiFactory {

  // todo: should this be exposed to all?
  def makeAkkaClient(akkaLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): SequenceManagerApi =
    new SequenceManagerImpl(akkaLocation)

  def makeHttpClient(httpLocation: HttpLocation, tokenFactory: () => Option[String])(implicit
      actorSystem: ActorSystem[_]
  ): SequenceManagerApi = {
    import SequenceManagerHttpCodec._

    val baseUri    = httpLocation.uri.toString
    val postUri    = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val postClient = new HttpPostTransport[SequenceManagerPostRequest](postUri, ContentType.Json, tokenFactory)
    new SequenceManagerClient(postClient)
  }
}
