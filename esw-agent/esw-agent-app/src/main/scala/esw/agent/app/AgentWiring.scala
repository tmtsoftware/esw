package esw.agent.app

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.{ProcessExecutor, ProcessOutput}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
// $COVERAGE-OFF$
class AgentWiring(prefix: Prefix, agentSettings: AgentSettings) {
  lazy val log: Logger = {
    actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)
    new LoggerFactory(prefix).getLogger
  }

  private val agentConnection: AkkaConnection = AkkaConnection(ComponentId(prefix, ComponentType.Machine))

  lazy val actorRuntime: ActorRuntime = new ActorRuntime()
    .withShutdownHook {
      log.warn("agent is shutting down. unregistering agent")
      Await.result(locationService.unregister(agentConnection), 2.seconds)
      log.info("agent unregistered due to coordinatedShutdown")
    }

  import actorRuntime.typedSystem
  implicit lazy val scheduler: Scheduler         = typedSystem.scheduler
  implicit lazy val ec: ExecutionContextExecutor = typedSystem.executionContext
  lazy val locationService: LocationService      = HttpLocationServiceFactory.makeLocalClient
  lazy val processOutput                         = new ProcessOutput
  lazy val processExecutor                       = new ProcessExecutor(processOutput, agentSettings, log)
  lazy val agentActor                            = new AgentActor(locationService, processExecutor, agentSettings, log)

  implicit private val timeout: Timeout = Timeout(10.seconds)
  lazy val lazyAgentRegistration: Future[RegistrationResult] =
    locationService.register(AkkaRegistration(agentConnection, agentRef.toURI))

  lazy val agentRef: ActorRef[AgentCommand] =
    Await.result(typedSystem ? (Spawn(agentActor.behavior(AgentState.empty), "agent-actor", Props.empty, _)), timeout.duration)
}
// $COVERAGE-ON$
