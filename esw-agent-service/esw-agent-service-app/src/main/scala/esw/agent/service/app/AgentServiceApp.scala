package esw.agent.service.app

// $COVERAGE-OFF$
import org.apache.pekko.actor.CoordinatedShutdown.UnknownReason
import caseapp.core.RemainingArgs
import csw.location.client.utils.LocationServerStatus
import esw.agent.service.app.AgentServiceAppCommand.StartCommand
import esw.commons.cli.EswCommand
import esw.constants.CommonTimeouts

import scala.concurrent.Await
import scala.util.control.NonFatal

/*
 * The main app to start Agent Service
 */
object AgentServiceApp extends EswCommand[AgentServiceAppCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentServiceAppCommand, args: RemainingArgs): Unit = {
    LocationServerStatus.requireUpLocally()
    command match {
      case StartCommand(port) => start(port)
    }
  }

  def start(port: Option[Int], startLogging: Boolean = true): AgentServiceWiring = {
    val httpWiring = new AgentServiceWiring(port)
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

// $COVERAGE-ON$
