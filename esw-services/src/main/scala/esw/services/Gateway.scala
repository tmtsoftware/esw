package esw.services

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import esw.gateway.server.{GatewayMain, GatewayWiring}
import esw.services.utils.PathUtils

import java.nio.file.Path

object Gateway {

  def service(enable: Boolean, maybeCommandRoleConfigPath: Option[Path]): ManagedService[GatewayWiring] = {
    val commandRoleConfigPath =
      maybeCommandRoleConfigPath.getOrElse(PathUtils.getResourcePath("commandRoles.conf"))
    ManagedService(
      "gateway",
      enable,
      () => startGateway(commandRoleConfigPath),
      stopGateway
    )
  }

  private def startGateway(commandRoleConfigPath: Path): GatewayWiring = {
    GatewayMain.start(None, local = true, commandRoleConfigPath, metricsEnabled = true, startLogging = true)
  }

  private def stopGateway(wiring: GatewayWiring): Unit = wiring.actorRuntime.shutdown(ActorSystemTerminateReason)
}
