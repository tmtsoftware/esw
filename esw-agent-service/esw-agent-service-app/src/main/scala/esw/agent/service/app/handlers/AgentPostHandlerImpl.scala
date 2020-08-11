package esw.agent.service.app.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esw.agent.service.api.AgentService
import esw.agent.service.api.codecs.AgentHttpCodecs._
import esw.agent.service.api.protocol.AgentPostRequest
import esw.agent.service.api.protocol.AgentPostRequest.{SpawnSequenceComponent, SpawnSequenceManager, StopComponent}
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class AgentPostHandlerImpl(agentService: AgentService) extends HttpPostHandler[AgentPostRequest] with ServerHttpCodecs {

  override def handle(request: AgentPostRequest): Route =
    request match {
      case SpawnSequenceComponent(agentPrefix, componentName, version) =>
        complete(agentService.spawnSequenceComponent(agentPrefix, componentName, version))

      case SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version) =>
        complete(agentService.spawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version))

      case StopComponent(agentPrefix, componentId) => complete(agentService.stopComponent(agentPrefix, componentId))
    }
}
