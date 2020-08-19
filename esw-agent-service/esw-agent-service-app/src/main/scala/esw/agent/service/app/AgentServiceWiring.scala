package esw.agent.service.app

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.codecs.AgentHttpCodecs
import esw.agent.service.app.handlers.AgentServicePostHandler
import esw.agent.service.impl.AgentServiceImpl
import esw.commons.utils.location.LocationServiceUtil
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.impl.post.PostRouteFactory

class AgentServiceWiring(port: Option[Int] = None) extends AgentHttpCodecs {

  lazy val prefix: Prefix                                          = Prefix(ESW, "agent_service")
  private lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")

  private[agent] lazy val wiring = new ServerWiring(port, Some(prefix), actorSystem = actorSystem)

  import wiring._
  import cswWiring.actorRuntime._
  import cswWiring.locationService

  private val securityDirective = SecurityDirectives(actorSystem.settings.config, locationService)

  private val locationServiceUtil = new LocationServiceUtil(locationService)
  private lazy val agentService   = new AgentServiceImpl(locationServiceUtil)
  private lazy val route: Route =
    new PostRouteFactory("post-endpoint", new AgentServicePostHandler(agentService, securityDirective)).make()

  lazy val httpService = new HttpService(logger, locationService, route, settings, cswWiring.actorRuntime)

  private[esw] def start() = httpService.startAndRegisterServer()

  private[esw] def stop() = shutdown(UnknownReason)
}
