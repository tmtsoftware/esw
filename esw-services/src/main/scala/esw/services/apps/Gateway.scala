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

// This class is created to start and stop the Gateway
object Gateway {

  // Creates an instance of ManagedService with start and stop hook for the Gateway
  def service(enable: Boolean, maybeCommandRoleConfigPath: Option[Path]): ManagedService[GatewayWiring] = {
    ManagedService(
      "gateway",
      enable,
      () => startGateway(getConfig(maybeCommandRoleConfigPath)),
      stopGateway
    )
  }

  // returns the given command role config's path
  // if not given any then returns the default command role config's path present in the resources
  private def getConfig(maybeCommandRoleConfigPath: Option[Path]): Path =
    maybeCommandRoleConfigPath.getOrElse {
      GREEN.println("Using default command role config for gateway.")
      FileUtils.cpyFileToTmpFromResource("commandRoles.conf")
    }

  // This method is for starting the Gateway and being called in the start hook for the Gateway
  private def startGateway(commandRoleConfigPath: Path): GatewayWiring =
    GatewayMain.StartCommand.start(
      Some(SocketUtils.getFreePort),
      local = true,
      commandRoleConfigPath,
      metricsEnabled = true,
      startLogging = true
    )

  // This method is for stopping the Gateway and being called in the stop hook for the Gateway
  private def stopGateway(wiring: GatewayWiring): Unit =
    Await.result(wiring.actorRuntime.shutdown(ActorSystemTerminateReason), CommonTimeouts.Wiring)

}
