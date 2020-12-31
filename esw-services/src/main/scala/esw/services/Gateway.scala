package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import esw.gateway.server.{GatewayMain, GatewayWiring}

import java.nio.file.Path

object Gateway {

  def service(enable: Boolean, commandRoleConfigPath: Option[Path]): ManagedService[GatewayWiring] = {
    ManagedService(
      "gateway",
      enable,
      () => startGateway(commandRoleConfigPath),
      stopGateway
    )
  }

  private def startGateway(commandRoleConfigPath: Option[Path]): Option[GatewayWiring] = {
    commandRoleConfigPath.map(p => GatewayMain.start(None, local = true, p, metricsEnabled = true, startLogging = true))
  }

  private def stopGateway(wiring: GatewayWiring): Unit = wiring.actorRuntime.shutdown(ActorSystemTerminateReason)
}
