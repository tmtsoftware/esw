package esw.agent.akka.app

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import esw.agent.akka.app.process.{ProcessExecutor, ProcessManager, ProcessOutput}
import esw.agent.akka.client.AgentCommand

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

// $COVERAGE-OFF$
class AgentWiring(prefix: Prefix, agentSettings: AgentSettings) {
  implicit lazy val timeout: Timeout = Timeout(10.seconds)
  implicit lazy val log: Logger      = new LoggerFactory(prefix).getLogger

  private[agent] val agentConnection: AkkaConnection = AkkaConnection(ComponentId(prefix, ComponentType.Machine))

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  lazy val actorRuntime: ActorRuntime                      = new ActorRuntime(actorSystem)

  import actorRuntime.typedSystem
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  lazy val processOutput                    = new ProcessOutput()
  lazy val processExecutor                  = new ProcessExecutor(processOutput)
  lazy val processManager                   = new ProcessManager(locationService, processExecutor, agentSettings)
  lazy val agentActor                       = new AgentActor(processManager)

  lazy val lazyAgentRegistration: Future[RegistrationResult] =
    locationService.register(AkkaRegistrationFactory.make(agentConnection, agentRef))

  lazy val agentRef: ActorRef[AgentCommand] =
    Await.result(typedSystem ? (Spawn(agentActor.behavior(AgentState.empty), "agent-actor", Props.empty, _)), timeout.duration)
}
// $COVERAGE-ON$
