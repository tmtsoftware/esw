package esw.agent.service.app

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.app.handlers.AgentServicePostHandler
import esw.agent.service.impl.AgentServiceImpl
import esw.commons.utils.location.LocationServiceUtil
import esw.http.core.wiring.{ActorRuntime, HttpService, ServerWiring}
import msocket.impl.post.PostRouteFactory

class AgentServiceWiring(port: Option[Int] = None) extends AgentServiceCodecs {

  lazy val prefix: Prefix                                          = Prefix(ESW, "agent_service")
  private lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  lazy val actorRuntime                                            = new ActorRuntime(actorSystem)
  import actorRuntime._

  private[agent] lazy val wiring = new ServerWiring(port, Some(prefix), actorSystem = actorSystem)

  import wiring._

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)
  private val securityDirective             = SecurityDirectives(actorSystem.settings.config, locationService)

  private val locationServiceUtil = new LocationServiceUtil(locationService)
  private lazy val agentService   = new AgentServiceImpl(locationServiceUtil)
  private lazy val route: Route =
    new PostRouteFactory("post-endpoint", new AgentServicePostHandler(agentService, securityDirective)).make()

  lazy val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

  private[app] def start() = httpService.startAndRegisterServer()

  private[esw] def stop() = shutdown(UnknownReason)
}
