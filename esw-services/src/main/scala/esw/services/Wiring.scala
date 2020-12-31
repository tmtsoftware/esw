package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import csw.services.utils.ColoredConsole._
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.constants.SequenceManagerTimeouts
import esw.gateway.server.{GatewayMain, GatewayWiring}
import esw.services.Command.Start
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse
import esw.sm.app.{SequenceManagerApp, SequenceManagerWiring}
import io.bullet.borer.Json

import java.io.File
import java.nio.file.Path
import scala.concurrent.Await

class Wiring(startCmd: Start) {
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  lazy val agentApp: ManagedService[AgentWiring] =
    ManagedService("agent", startCmd.agentPrefix.nonEmpty, () => startAgent(startCmd.agentPrefix), stopAgent)
  lazy val gatewayService: ManagedService[GatewayWiring] =
    ManagedService(
      "gateway",
      startCmd.commandRoleConfig.nonEmpty,
      () => startGateway(startCmd.commandRoleConfig),
      stopGateway
    )
  lazy val smService: ManagedService[SequenceManagerWiring] =
    ManagedService(
      "sequence-manager",
      startCmd.obsModeConfig.nonEmpty,
      () => startSM(startCmd.obsModeConfig, startCmd.provisionConfig),
      stopSM
    )

  lazy val serviceList = List(agentApp, gatewayService, smService)

  def start(): Unit = serviceList.foreach(_.start())

  def stop(): Unit = serviceList.foreach(_.stop())

  private def startAgent(maybePrefix: Option[Prefix]): Option[AgentWiring] = {
    maybePrefix.map(p => AgentApp.start(AgentSettings(p, ConfigFactory.load())))
  }

  private def stopAgent(wiring: AgentWiring): Unit = {
    wiring.actorSystem.terminate()
    wiring.actorSystem.whenTerminated
  }

  private def startGateway(commandRoleConfigPath: Option[Path]): Option[GatewayWiring] = {
    commandRoleConfigPath.map(p => GatewayMain.start(None, local = true, p, metricsEnabled = true, startLogging = true))
  }

  private def stopGateway(wiring: GatewayWiring): Unit = wiring.actorRuntime.shutdown(ActorSystemTerminateReason)

  private def startSM(obsModeConfigPath: Option[Path], provisionConfigPath: Option[Path]): Option[SequenceManagerWiring] =
    obsModeConfigPath.map(p => {
      val wiring = SequenceManagerApp.start(p, isConfigLocal = true, None, startLogging = true)
      provisionConfigPath.foreach(p => provision(wiring, p))
      wiring
    })

  private def stopSM(smWiring: SequenceManagerWiring) = smWiring.shutdown(ActorSystemTerminateReason)

  private def provision(wiring: SequenceManagerWiring, p: Path): Unit = {
    val provisionResponse =
      Await.result(wiring.sequenceManager.provision(makeProvisionConfig(p)), SequenceManagerTimeouts.Provision)
    provisionResponse match {
      case ProvisionResponse.Success          => GREEN.println("Sequence components provisioned successfully")
      case failure: ProvisionResponse.Failure => RED.println(s"Provision failed: ${failure.getMessage}")
    }
  }

  private def makeProvisionConfig(configPath: Path): ProvisionConfig = {
    import esw.sm.api.codecs.SequenceManagerCodecs.provisionConfigCodec
    val config    = ConfigFactory.parseFile(new File(configPath.toUri))
    val configStr = config.root().render(ConfigRenderOptions.concise())
    Json.decode(configStr.getBytes).to[ProvisionConfig].value
  }
}
