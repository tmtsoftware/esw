package esw.agent.service.app

import akka.actor.CoordinatedShutdown.UnknownReason
import caseapp.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import esw.commons.Timeouts
import esw.commons.cli.EswCommandApp

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

object AgentServiceApp extends EswCommandApp[AgentServiceAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentServiceAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    start()
  }

  def start(): Unit = {
    val httpWiring = new AgentServiceWiring()
    try {
      httpWiring.actorRuntime.startLogging(progName, appVersion)
      Await.result(httpWiring.start(), 10.seconds)
    }
    catch {
      case NonFatal(e) =>
        Await.result(httpWiring.actorRuntime.shutdown(UnknownReason), Timeouts.DefaultTimeout)
        throw e
    }
  }
}
