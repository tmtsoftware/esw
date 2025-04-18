package esw.agent.service.api.client

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Path
import csw.location.api.models.HttpLocation
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.protocol.AgentServiceRequest
import msocket.api.ContentType
import msocket.http.post.HttpPostTransport

/**
 * This is a factory to create instance of agent service's http client
 */
object AgentServiceClientFactory {

  def apply(httpLocation: HttpLocation, tokenFactory: () => Option[String])(implicit
      actorSystem: ActorSystem[?]
  ): AgentServiceApi = {
    import AgentServiceCodecs.*

    val baseUri    = httpLocation.uri.toString
    val postUri    = Uri(baseUri).withPath(Path("/post-endpoint")).toString()
    val postClient = new HttpPostTransport[AgentServiceRequest](postUri, ContentType.Json, tokenFactory)
    new AgentServiceClient(postClient)
  }
}
