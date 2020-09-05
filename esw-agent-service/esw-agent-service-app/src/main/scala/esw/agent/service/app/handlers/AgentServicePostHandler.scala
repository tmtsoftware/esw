package esw.agent.service.app.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs._
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{KillComponent, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.app.auth.EswUserRolePolicy
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

class AgentServicePostHandler(agentService: AgentServiceApi, securityDirective: SecurityDirectives)
    extends HttpPostHandler[AgentServiceRequest]
    with ServerHttpCodecs {

  import agentService._
  override def handle(request: AgentServiceRequest): Route =
    request match {
      case SpawnSequenceComponent(agentPrefix, componentName, version) =>
        sPost(complete(spawnSequenceComponent(agentPrefix, componentName, version)))

      case SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version) =>
        sPost(complete(spawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version)))

      case KillComponent(connection) =>
        sPost(complete(killComponent(connection)))
    }

  private def sPost(route: => Route): Route = securityDirective.sPost(EswUserRolePolicy())(_ => route)
}
