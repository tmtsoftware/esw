package esw.agent.service.api.client

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import csw.location.api.models.HttpLocation
import esw.agent.service.api.AgentService
import esw.agent.service.api.protocol.AgentPostRequest
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport
import esw.agent.service.api.codecs.AgentHttpCodecs

object AgentServiceClientFactory {

  def apply(httpLocation: HttpLocation, tokenFactory: () => Option[String])(implicit
      actorSystem: ActorSystem[_]
  ): AgentService = {
    import AgentHttpCodecs._

    val baseUri    = httpLocation.uri.toString
    val postUri    = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val postClient = new HttpPostTransport[AgentPostRequest](postUri, ContentType.Json, tokenFactory)
    new AgentServiceClient(postClient)
  }
}
