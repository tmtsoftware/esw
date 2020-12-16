package esw.agent.service.app.handlers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs._
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest._
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

      case SpawnEventServer(agentPrefix, sentinelConfPath, port, version) =>
        sPost(complete(spawnEventServer(agentPrefix, sentinelConfPath, port, version)))

      case SpawnAlarmServer(agentPrefix, sentinelConfPath, port, version) =>
        sPost(complete(spawnAlarmServer(agentPrefix, sentinelConfPath, port, version)))

      case SpawnPostgres(agentPrefix, pgDataConfPath, port, dbUnixSocketDirs, version) =>
        sPost(complete(spawnPostgres(agentPrefix, pgDataConfPath, port, dbUnixSocketDirs, version)))

      case KillComponent(componentId) =>
        sPost(complete(killComponent(componentId)))
    }

  private def sPost(route: => Route): Route = securityDirective.sPost(EswUserRolePolicy())(_ => route)
}
