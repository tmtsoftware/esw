package esw.services

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.config.api.scaladsl.ConfigService
import csw.config.api.{ConfigData, TokenFactory}
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.*
import esw.agent.pekko.app.AgentWiring
import esw.agent.service.api.models.SpawnResponse
import esw.agent.service.app.AgentServiceWiring
import esw.commons.extensions.FutureExt.FutureOps
import esw.commons.utils.aas.Keycloak
import esw.commons.utils.config.ConfigServiceExt
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.gateway.server.GatewayWiring
import esw.http.core.wiring.HttpService
import esw.services.apps.{Agent, AgentService, Gateway, SequenceManager}
import esw.services.cli.Command
import esw.services.cli.Command.{StartOptions, StartEngUIServicesOptions}
import esw.services.internal.ManagedService

import java.nio.file.Path

// wiring class which provide method to start/stop services by reading the command and
// parsing which services needed to start/stop
class Wiring(cmd: Command) {

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  private lazy val systemConfig: Config                             = ConfigFactory.load()

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  private lazy val serviceList: List[ManagedService[?]] = cmd match {
    case s: StartOptions              => getServiceListForStart(s)
    case s: StartEngUIServicesOptions => getServiceListForEngUIBackend(s)
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

  private lazy val obsModeConfRemotePath = Path.of(systemConfig.getString("esw.sm.obsModeConfigPath"))
  private lazy val configData            = ConfigData.fromString(FileUtils.readResource("smObsModeConfig.conf"))
  private lazy val provisionData         = ConfigData.fromString(FileUtils.readResource("smProvisionConfig.json"))
  private lazy val configServiceExt      = new ConfigServiceExt(configService)

  private lazy val sequencerScriptsSha = "153b6748e0"

  private lazy val eswVersionDefault =
    Option(classOf[HttpService].getPackage.getSpecificationVersion).getOrElse("0.1.0-SNAPSHOT")

  private lazy val (scriptVersion, eswVersion) = cmd match {
    case _: StartOptions => (sequencerScriptsSha, eswVersionDefault)
    case s: StartEngUIServicesOptions =>
      (s.scriptsVersion.getOrElse(sequencerScriptsSha), s.eswVersion.getOrElse(eswVersionDefault))
  }

  private lazy val VersionConf = {
    println(s"${Console.YELLOW}Using versions: ESW=$eswVersion, sequencerScriptsVersion=$scriptVersion${Console.RESET}")
    s"""
         |scripts = $scriptVersion
         |esw = $eswVersion
         |""".stripMargin
  }

  private lazy val versionConfigData = ConfigData.fromString(VersionConf)

  def start(): Unit = {
    configServiceExt.saveConfig(obsModeConfRemotePath, configData)
    configServiceExt.saveConfig(Path.of("/tmt/osw/version.conf"), versionConfigData)
    configServiceExt.saveConfig(Path.of("/tmt/esw/smProvisionConfig.json"), provisionData)
    serviceList.foreach(_.start())
  }

  def stop(): Unit = serviceList.foreach(_.stop())

  private def getServiceListForStart(cmd: StartOptions) = {
    val agentPrefix: Prefix                              = cmd.agentPrefix.getOrElse(Prefix(ESW, "primary"))
    val agentApp: ManagedService[AgentWiring]            = Agent.service(cmd.agent, agentPrefix, systemConfig, cmd.hostConfigPath)
    val agentService: ManagedService[AgentServiceWiring] = AgentService.service(cmd.agentService)
    val gatewayService: ManagedService[GatewayWiring]    = Gateway.service(cmd.gateway, cmd.commandRoleConfig)
    val smService: ManagedService[SpawnResponse] =
      new SequenceManager(locationService).service(cmd.sequenceManager, cmd.obsModeConfig, cmd.simulation)

    val serviceList = List(agentApp, agentService, gatewayService, smService)
    serviceList
  }

  private def getServiceListForEngUIBackend(command: StartEngUIServicesOptions): List[ManagedService[?]] = {
    // start agents as per provision config
    val agentApp1 = Agent.service(enable = true, Prefix(ESW, "machine1"), systemConfig)
    val agentApp2 = Agent.service(enable = true, Prefix(AOESW, "machine1"), systemConfig)
    val agentApp3 = Agent.service(enable = true, Prefix(IRIS, "machine1"), systemConfig)
    val agentApp4 = Agent.service(enable = true, Prefix(TCS, "machine1"), systemConfig)
    val agentApp5 = Agent.service(enable = true, Prefix(WFOS, "machine1"), systemConfig)

    val agentService   = AgentService.service(enable = true)
    val gatewayService = Gateway.service(enable = true, None)
    val smService =
      new SequenceManager(locationService).service(
        enable = true,
        maybeObsModeConfigPath = command.obsModeConfig,
        simulation = command.smSimulationMode
      )

    val serviceList = List(agentApp1, agentApp2, agentApp3, agentApp4, agentApp5, agentService, gatewayService, smService)
    serviceList
  }
}
