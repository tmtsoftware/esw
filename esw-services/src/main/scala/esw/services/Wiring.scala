package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import esw.agent.akka.app.{AgentApp, AgentSettings, AgentWiring}
import esw.gateway.server.{GatewayMain, GatewayWiring}
import esw.services.Command.Start

import java.nio.file.Path

class Wiring(startCmd: Start) {
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())

  lazy val agentApp: ManagedService[AgentWiring] =
    ManagedService("agent", startCmd.agentPrefix.nonEmpty, () => startAgent(startCmd.agentPrefix), stopAgent)
  lazy val gatewayService: ManagedService[GatewayWiring] =
    ManagedService(
      "gateway",
      startCmd.commandRoleConfigPath.nonEmpty,
      () => startGateway(startCmd.commandRoleConfigPath),
      stopGateway
    )

  lazy val serviceList = List(agentApp, gatewayService)

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
}
