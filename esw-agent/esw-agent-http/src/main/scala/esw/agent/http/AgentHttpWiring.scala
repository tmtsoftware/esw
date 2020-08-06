package esw.agent.http

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.codecs.AgentHttpCodecs
import esw.agent.http.handlers.AgentPostHandlerImpl
import esw.agent.http.impl.AgentServiceImpl
import esw.http.core.wiring.{HttpService, ServerWiring}
import msocket.impl.post.PostRouteFactory

class AgentHttpWiring(port: Option[Int]) extends AgentHttpCodecs {

  private val prefix                                               = Prefix(ESW, "agent_service")
  private lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")

  private lazy val wiring = new ServerWiring(port, Some(prefix), actorSystem = actorSystem)

  import wiring._
  import cswWiring.actorRuntime._
  import cswWiring.locationService

  private lazy val agentService = new AgentServiceImpl(locationService)
  private lazy val route: Route = new PostRouteFactory("post-endpoint", new AgentPostHandlerImpl(agentService)).make()

  lazy val httpService = new HttpService(logger, locationService, route, settings, cswWiring.actorRuntime)
}
