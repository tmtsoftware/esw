package esw.agent.akka.app

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.config.client.commons.ConfigUtils
import csw.config.client.scaladsl.ConfigClientFactory
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
import esw.agent.akka.client.models.ContainerConfig
import esw.agent.akka.client.{AgentClient, AgentCommand}
import esw.commons.utils.config.VersionManager
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts

import java.nio.file.Path
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

// $COVERAGE-OFF$
class AgentWiring(agentSettings: AgentSettings) {

  val prefix: Prefix = agentSettings.prefix

  implicit lazy val timeout: Timeout = CommonTimeouts.Wiring
  implicit lazy val log: Logger      = new LoggerFactory(prefix).getLogger

  private[agent] val agentConnection: AkkaConnection = AkkaConnection(ComponentId(prefix, ComponentType.Machine))

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "agent-app")
  lazy val actorRuntime: ActorRuntime                      = new ActorRuntime(actorSystem)

  import actorRuntime.typedSystem
  implicit lazy val ec: ExecutionContext    = actorRuntime.typedSystem.executionContext
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val configClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val configUtils         = new ConfigUtils(configClientService)
  private lazy val versionManager      = new VersionManager(configUtils)

  lazy val processOutput   = new ProcessOutput()
  lazy val processExecutor = new ProcessExecutor(processOutput)
  lazy val processManager  = new ProcessManager(locationService, versionManager, processExecutor, agentSettings)
  lazy val agentActor      = new AgentActor(processManager)

  lazy val lazyAgentRegistration: Future[RegistrationResult] =
    locationService.register(AkkaRegistrationFactory.make(agentConnection, agentRef))

  lazy val agentRef: ActorRef[AgentCommand] =
    Await.result(typedSystem ? (Spawn(agentActor.behavior, "agent-actor", Props.empty, _)), timeout.duration)

  def spawnContainers(path: Path, isConfigLocal: Boolean): Unit = {
    val locationServiceUtil = new LocationServiceUtil(locationService)
    AgentClient.make(prefix, locationServiceUtil).map {
      case Left(error) => log.error(s"Unable to find agent: ${error.msg}")
      case Right(agentClient) =>
        val hostConfig = getHostConfig(configUtils, path, isConfigLocal)
        Await.result(agentClient.spawnContainers(hostConfig), CommonTimeouts.Wiring)
    }
  }

  private def getHostConfig(configUtils: ConfigUtils, path: Path, isConfigLocal: Boolean)(implicit
      ec: ExecutionContext
  ): List[ContainerConfig] = {
    val containerConfigsF = configUtils
      .getConfig(path, isConfigLocal)
      .map(config => {
        config.getConfigList("containers").asScala.map(c => ContainerConfig(c)).toList
      })
    Await.result(containerConfigsF, CommonTimeouts.Wiring)
  }
}
// $COVERAGE-ON$
