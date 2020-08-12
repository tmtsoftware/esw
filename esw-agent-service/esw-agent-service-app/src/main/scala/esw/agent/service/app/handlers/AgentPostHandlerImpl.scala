package esw.agent.service.app.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.agent.service.api.AgentService
import esw.agent.service.api.codecs.AgentHttpCodecs._
import esw.agent.service.api.protocol.AgentPostRequest
import esw.agent.service.api.protocol.AgentPostRequest.{SpawnSequenceComponent, SpawnSequenceManager, StopComponent}
import esw.agent.service.app.auth.EswUserRolePolicy
import msocket.impl.post.{HttpPostHandler, ServerHttpCodecs}

class AgentPostHandlerImpl(agentService: AgentService, securityDirective: SecurityDirectives)
    extends HttpPostHandler[AgentPostRequest]
    with ServerHttpCodecs {

  import agentService._
  override def handle(request: AgentPostRequest): Route =
    request match {
      case SpawnSequenceComponent(agentPrefix, componentName, version) =>
        sPost(complete(spawnSequenceComponent(agentPrefix, componentName, version)))

      case SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version) =>
        sPost(complete(spawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version)))

      case StopComponent(agentPrefix, componentId) =>
        sPost(complete(stopComponent(agentPrefix, componentId)))
    }

  private def sPost(route: => Route): Route = securityDirective.sPost(EswUserRolePolicy())(_ => route)
}
