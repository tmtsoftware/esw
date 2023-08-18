package esw.agent.pekko.app

import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import org.apache.pekko.util.Timeout
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import esw.agent.pekko.app.process.{ProcessExecutor, ProcessManager, ProcessOutput}
import esw.agent.pekko.client.AgentCommand
import esw.commons.utils.config.VersionManager
import esw.constants.CommonTimeouts

import scala.concurrent.{Await, ExecutionContext, Future}

// $COVERAGE-OFF$
class AgentWiring(agentSettings: AgentSettings, hostConfigPath: Option[String], isConfigLocal: Boolean) {

  val prefix: Prefix = agentSettings.prefix

  implicit lazy val timeout: Timeout = CommonTimeouts.Wiring
  implicit lazy val log: Logger      = new LoggerFactory(prefix).getLogger

  private[agent] val agentConnection: PekkoConnection = PekkoConnection(ComponentId(prefix, ComponentType.Machine))

  val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  final lazy val actorRuntime: ActorRuntime                      = new ActorRuntime(actorSystem)

  import actorRuntime.typedSystem
  implicit lazy val ec: ExecutionContext    = actorRuntime.typedSystem.executionContext
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val configClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val configUtils         = new ConfigUtils(configClientService)
  private lazy val versionManager      = new VersionManager(agentSettings.versionConfPath, configUtils)

  lazy val processOutput   = new ProcessOutput()
  lazy val processExecutor = new ProcessExecutor(processOutput)
  lazy val processManager  = new ProcessManager(locationService, versionManager, processExecutor, agentSettings)
  lazy val agentActor      = new AgentActor(processManager, configUtils, hostConfigPath, isConfigLocal)

  lazy val lazyAgentRegistration: Future[RegistrationResult] =
    locationService.register(PekkoRegistrationFactory.make(agentConnection, agentRef))

  lazy val agentRef: ActorRef[AgentCommand] =
    Await.result(typedSystem ? (Spawn(agentActor.behavior, "agent-actor", Props.empty, _)), timeout.duration)
}
// $COVERAGE-ON$
