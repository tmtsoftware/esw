package esw.agent.service.app

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.codecs.AgentHttpCodecs
import esw.agent.service.app.handlers.AgentPostHandlerImpl
import esw.agent.service.impl.AgentServiceImpl
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.impl.post.PostRouteFactory

class AgentHttpWiring(port: Option[Int]) extends AgentHttpCodecs {

  lazy val prefix: Prefix                                  = Prefix(ESW, "agent_service")
  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")

  private lazy val wiring = new ServerWiring(port, Some(prefix), actorSystem = actorSystem)

  import wiring._
  import cswWiring.actorRuntime._
  import cswWiring.locationService

  private val securityDirective = SecurityDirectives(actorSystem.settings.config, locationService)

  private lazy val agentService = new AgentServiceImpl(locationService)
  private lazy val route: Route =
    new PostRouteFactory("post-endpoint", new AgentPostHandlerImpl(agentService, securityDirective)).make()

  lazy val httpService = new HttpService(logger, locationService, route, settings, cswWiring.actorRuntime)
}
