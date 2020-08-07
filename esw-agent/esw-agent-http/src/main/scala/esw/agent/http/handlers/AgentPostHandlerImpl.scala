package esw.agent.http.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.agent.api.codecs.AgentHttpCodecs._
import esw.agent.api.protocol.{AgentPostRequest, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.http.api.AgentService
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class AgentPostHandlerImpl(agentService: AgentService) extends HttpPostHandler[AgentPostRequest] with ServerHttpCodecs {

  override def handle(request: AgentPostRequest): Route =
    request match {
      case SpawnSequenceComponent(agentPrefix, prefix, version) =>
        complete(agentService.spawnSequenceComponent(agentPrefix, prefix, version))

      case SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version) =>
        complete(agentService.spawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version))
    }
}
