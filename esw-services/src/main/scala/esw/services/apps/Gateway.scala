package esw.services.apps

import akka.actor.CoordinatedShutdown.ActorSystemTerminateReason
import csw.network.utils.SocketUtils
import csw.services.utils.ColoredConsole.GREEN
import esw.commons.utils.files.FileUtils
import esw.constants.CommonTimeouts
import esw.gateway.server.{GatewayMain, GatewayWiring}
import esw.services.internal.ManagedService

import java.nio.file.Path
import scala.concurrent.Await

object Gateway {

  def service(enable: Boolean, maybeCommandRoleConfigPath: Option[Path]): ManagedService[GatewayWiring] = {
    ManagedService(
      "gateway",
      enable,
      () => startGateway(getConfig(maybeCommandRoleConfigPath)),
      stopGateway
    )
  }

  private def getConfig(maybeCommandRoleConfigPath: Option[Path]): Path =
    maybeCommandRoleConfigPath.getOrElse {
      GREEN.println("Using default command role config for gateway.")
      FileUtils.cpyFileToTmpFromResource("commandRoles.conf")
    }

  private def startGateway(commandRoleConfigPath: Path): GatewayWiring =
    GatewayMain.start(
      Some(SocketUtils.getFreePort),
      local = true,
      commandRoleConfigPath,
      metricsEnabled = true,
      startLogging = true
    )

  private def stopGateway(wiring: GatewayWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)

}
