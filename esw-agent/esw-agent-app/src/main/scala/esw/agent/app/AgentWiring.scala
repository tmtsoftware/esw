package esw.agent.app

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.{ProcessExecutor, ProcessOutput}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}
// $COVERAGE-OFF$
class AgentWiring(prefix: Prefix, agentSettings: AgentSettings) {
  implicit lazy val timeout: Timeout = Timeout(10.seconds)
  lazy val log: Logger               = new LoggerFactory(prefix).getLogger

  private[agent] val agentConnection: AkkaConnection = AkkaConnection(ComponentId(prefix, ComponentType.Machine))

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  lazy val actorRuntime: ActorRuntime                      = new ActorRuntime(actorSystem)

  import actorRuntime.typedSystem
  implicit lazy val scheduler: Scheduler    = typedSystem.scheduler
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  lazy val processOutput                    = new ProcessOutput()
  lazy val processExecutor                  = new ProcessExecutor(processOutput, log)
  lazy val agentActor                       = new AgentActor(locationService, processExecutor, agentSettings, log)

  lazy val lazyAgentRegistration: Future[RegistrationResult] =
    locationService.register(AkkaRegistrationFactory.make(agentConnection, agentRef.toURI))

  lazy val agentRef: ActorRef[AgentCommand] =
    Await.result(typedSystem ? (Spawn(agentActor.behavior(AgentState.empty), "agent-actor", Props.empty, _)), timeout.duration)
}

object AgentWiring {
  private[esw] def make(
      prefix: Prefix,
      agentSettings: AgentSettings,
      _actorSystem: ActorSystem[SpawnProtocol.Command]
  ): AgentWiring =
    new AgentWiring(prefix, agentSettings) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem
    }
}

// $COVERAGE-ON$
