package esw.agent.service.app.handlers

import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs.*
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.*

import esw.commons.auth.AuthPolicies
import msocket.http.post.{HttpPostHandler, ServerHttpCodecs}

/**
 * This is the Http(POST) route handler written using msocket apis for Agent Service.
 *
 * @param agentService - an instance of agentServiceApi
 * @param securityDirective - security directive to deal with auth
 */
class AgentServicePostHandler(agentService: AgentServiceApi, securityDirective: SecurityDirectives)
    extends HttpPostHandler[AgentServiceRequest]
    with ServerHttpCodecs {

  import agentService.*
  override def handle(request: AgentServiceRequest): Route =
    request match {
      case SpawnSequenceComponent(agentPrefix, componentName, version) =>
        sPost(complete(spawnSequenceComponent(agentPrefix, componentName, version)))

      case SpawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version) =>
        sPost(complete(spawnSequenceManager(agentPrefix, obsModeConfigPath, isConfigLocal, version)))

      case SpawnContainers(agentPrefix, hostConfigPath, isConfigLocal) =>
        sPost(complete(spawnContainers(agentPrefix, hostConfigPath, isConfigLocal)))

      case KillComponent(componentId) =>
        sPost(complete(killComponent(componentId)))

      case GetAgentStatus => complete(getAgentStatus)
    }

  private def sPost(route: => Route): Route = securityDirective.sPost(AuthPolicies.eswUserRolePolicy)(_ => route)
}
