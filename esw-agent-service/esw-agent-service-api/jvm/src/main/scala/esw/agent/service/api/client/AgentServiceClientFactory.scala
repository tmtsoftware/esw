package esw.agent.service.api.client

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import csw.location.api.models.HttpLocation
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentHttpCodecs
import esw.agent.service.api.protocol.AgentPostRequest
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport

object AgentServiceClientFactory {

  def apply(httpLocation: HttpLocation, tokenFactory: () => Option[String])(implicit
      actorSystem: ActorSystem[_]
  ): AgentServiceApi = {
    import AgentHttpCodecs._

    val baseUri    = httpLocation.uri.toString
    val postUri    = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val postClient = new HttpPostTransport[AgentPostRequest](postUri, ContentType.Json, tokenFactory)
    new AgentServiceClient(postClient)
  }
}
