package esw.sm.api.actor.client

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path
import csw.location.api.models.{PekkoLocation, HttpLocation}
import esw.sm.api.SequenceManagerApi
import esw.sm.api.client.SequenceManagerClient
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.protocol.SequenceManagerRequest
import msocket.api.ContentType
import msocket.http.post.HttpPostTransport

object SequenceManagerApiFactory {

  // todo: should this be exposed to all?
  /**
   * Creates pekkoClient for the the Sequencer Manager
   *
   * @param pekkoLocation - pekko Location of the Sequencer
   * @param actorSystem - an Pekko ActorSystem
   *
   * @return an instance of [[esw.sm.api.SequenceManagerApi]]
   */
  def makePekkoClient(pekkoLocation: PekkoLocation)(implicit actorSystem: ActorSystem[_]): SequenceManagerApi =
    new SequenceManagerImpl(pekkoLocation)

  /**
   * Creates http client for the the Sequencer Manager
   *
   * @param httpLocation - http Location of the Sequencer
   * @param tokenFactory - a function that return the auth token
   * @param actorSystem - an Pekko ActorSystem
   *
   * @return an instance of [[esw.sm.api.SequenceManagerApi]]
   */
  def makeHttpClient(httpLocation: HttpLocation, tokenFactory: () => Option[String])(implicit
      actorSystem: ActorSystem[_]
  ): SequenceManagerApi = {
    import SequenceManagerServiceCodecs.*

    val baseUri    = httpLocation.uri.toString
    val postUri    = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val postClient = new HttpPostTransport[SequenceManagerRequest](postUri, ContentType.Json, tokenFactory)
    new SequenceManagerClient(postClient)
  }
}
