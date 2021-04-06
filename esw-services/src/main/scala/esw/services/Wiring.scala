package esw.services

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.config.api.scaladsl.ConfigService
import csw.config.api.{ConfigData, TokenFactory}
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import esw.agent.akka.app.AgentWiring
import esw.agent.service.api.models.SpawnResponse
import esw.agent.service.app.AgentServiceWiring
import esw.commons.extensions.FutureExt.FutureOps
import esw.commons.utils.aas.Keycloak
import esw.commons.utils.config.ConfigServiceExt
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.gateway.server.GatewayWiring
import esw.services.apps.{Agent, AgentService, Gateway, SequenceManager}
import esw.services.cli.Command
import esw.services.cli.Command.{Start, StartEngUIServices}
import esw.services.internal.ManagedService

import java.nio.file.Path

class Wiring(cmd: Command) {

  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private val systemConfig: Config                                  = ConfigFactory.load()

  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  private lazy val serviceList: List[ManagedService[_]] = cmd match {
    case s: Start              => getServiceListForStart(s)
    case s: StartEngUIServices => getServiceListForEngUIBackend(s)
  }

  private lazy val configService: ConfigService = ConfigClientFactory.adminApi(actorSystem, locationService, tokenFactory)
  private lazy val config                       = systemConfig.getConfig("csw")
  private lazy val configAdminUsername: String  = config.getString("configAdminUsername")
  private lazy val configAdminPassword: String  = config.getString("configAdminPassword")
  private lazy val keycloak                     = new Keycloak(locationService)(actorSystem.executionContext)

  private def tokenFactory: TokenFactory =
    new TokenFactory {
      override def getToken: String = keycloak.getToken(configAdminUsername, configAdminPassword).await(CommonTimeouts.Wiring)
    }

  private val obsModeConfRemotePath = Path.of(systemConfig.getString("esw.sm.obsModeConfigPath"))
  private val configData            = ConfigData.fromString(FileUtils.readResource("smObsModeConfig.conf"))
  private val provisionData         = ConfigData.fromString(FileUtils.readResource("smProvisionConfig.json"))
  private lazy val configServiceExt = new ConfigServiceExt(configService)
  private val VersionConf =
    s"""
      |scripts = 56a5375
      |
      |esw = 0.1.0-SNAPSHOT
      |
      |""".stripMargin

  private val versionConfigData = ConfigData.fromString(VersionConf)

  configServiceExt.saveConfig(obsModeConfRemotePath, configData)
  configServiceExt.saveConfig(Path.of("/tmt/osw/version.conf"), versionConfigData)
  configServiceExt.saveConfig(Path.of("/tmt/esw/smProvisionConfig.json"), provisionData)

  def start(): Unit = serviceList.foreach(_.start())

  def stop(): Unit = serviceList.foreach(_.stop())

  private def getServiceListForStart(cmd: Start) = {
    val agentPrefix: Prefix                              = cmd.agentPrefix.getOrElse(Prefix(ESW, "primary"))
    val agentApp: ManagedService[AgentWiring]            = Agent.service(cmd.agent, agentPrefix, systemConfig, cmd.hostConfigPath)
    val agentService: ManagedService[AgentServiceWiring] = AgentService.service(cmd.agentService)
    val gatewayService: ManagedService[GatewayWiring]    = Gateway.service(cmd.gateway, cmd.commandRoleConfig)
    val smService: ManagedService[SpawnResponse] =
      new SequenceManager(locationService).service(cmd.sequenceManager, cmd.obsModeConfig, cmd.simulation)

    val serviceList = List(agentApp, agentService, gatewayService, smService)
    serviceList
  }

  private def getServiceListForEngUIBackend(command: StartEngUIServices): List[ManagedService[_]] = {
    // start agents as per provision config
    val agentApp1: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(ESW, "machine1"), systemConfig)
    val agentApp2: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(AOESW, "machine1"), systemConfig)
    val agentApp3: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(IRIS, "machine1"), systemConfig)
    val agentApp4: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(TCS, "machine1"), systemConfig)
    val agentApp5: ManagedService[AgentWiring] = Agent.service(enable = true, Prefix(WFOS, "machine1"), systemConfig)

    val agentService: ManagedService[AgentServiceWiring] = AgentService.service(enable = true)

    val gatewayService: ManagedService[GatewayWiring] = Gateway.service(enable = true, None)

    val smService: ManagedService[SpawnResponse] =
      new SequenceManager(locationService).service(enable = true, None, simulation = command.smSimulationMode)

    val serviceList = List(agentApp1, agentApp2, agentApp3, agentApp4, agentApp5, agentService, gatewayService, smService)
    serviceList
  }
}
