package esw.agent.service.app

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentType
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.SocketUtils
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.app.handlers.AgentServicePostHandler
import esw.agent.service.impl.{AgentServiceImpl, AgentStatusUtil}
import esw.commons.utils.location.LocationServiceUtil
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import msocket.http.post.PostRouteFactory
import msocket.jvm.metrics.LabelExtractor

import scala.concurrent.Future

class AgentServiceWiring(_port: Option[Int] = None) extends AgentServiceCodecs {

  lazy val agentActorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  final lazy val actorRuntime                                   = new ActorRuntime(agentActorSystem)
  import actorRuntime._

  private lazy val config = agentActorSystem.settings.config
  lazy val prefix: Prefix = Prefix(ESW, "agent_service")
  private lazy val port   = _port.getOrElse(SocketUtils.getFreePort)
  lazy val settings       = new Settings(Some(port), Some(prefix), config, ComponentType.Service)

  private lazy val loggerFactory = new LoggerFactory(settings.httpConnection.prefix)
  lazy val logger: Logger        = loggerFactory.getLogger

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(agentActorSystem)
  private[esw] lazy val securityDirective   = SecurityDirectives(agentActorSystem.settings.config, locationService)

  private lazy val locationServiceUtil   = new LocationServiceUtil(locationService)
  private lazy val agentStatusUtil       = new AgentStatusUtil(locationServiceUtil)
  lazy val agentService: AgentServiceApi = new AgentServiceImpl(locationServiceUtil, agentStatusUtil)

  import LabelExtractor.Implicits.default
  private lazy val route: Route =
    new PostRouteFactory("post-endpoint", new AgentServicePostHandler(agentService, securityDirective)).make()

  lazy val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

  def start(): Future[(Http.ServerBinding, RegistrationResult)] = httpService.startAndRegisterServer()

  def stop(): Future[Done] = shutdown(UnknownReason)
}
