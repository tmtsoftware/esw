package esw.agent.service.app

import akka.actor.CoordinatedShutdown.UnknownReason
import caseapp.core.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import esw.commons.cli.EswCommandApp
import esw.constants.CommonTimeouts

import scala.concurrent.Await
import scala.util.control.NonFatal

object AgentServiceApp extends EswCommandApp[AgentServiceAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentServiceAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    start()
  }

  def start(startLogging: Boolean = true): AgentServiceWiring = {
    val httpWiring = new AgentServiceWiring()
    try {
      if (startLogging) httpWiring.actorRuntime.startLogging(progName, appVersion)
      Await.result(httpWiring.start(), CommonTimeouts.Wiring)
      httpWiring
    }
    catch {
      case NonFatal(e) =>
        Await.result(httpWiring.actorRuntime.shutdown(UnknownReason), CommonTimeouts.Wiring)
        throw e
    }
  }
}
