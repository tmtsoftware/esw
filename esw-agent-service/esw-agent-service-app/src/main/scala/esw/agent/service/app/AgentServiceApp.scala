package esw.agent.service.app

// $COVERAGE-OFF$
import org.apache.pekko.actor.CoordinatedShutdown.UnknownReason
import caseapp.{Command, RemainingArgs}
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import csw.location.client.utils.LocationServerStatus
import esw.agent.service.app.AgentServiceAppCommand.StartOptions
import esw.commons.cli.EswCommand
import esw.constants.CommonTimeouts

import scala.concurrent.Await
import scala.util.control.NonFatal

/*
 * The main app to start Agent Service
 */
object AgentServiceApp extends CommandsEntryPoint {
  def appName: String           = getClass.getSimpleName.dropRight(1) // remove $ from class name
  def appVersion: String        = BuildInfo.version
  override def progName: String = BuildInfo.name

  val StartCommand: Runner[StartOptions] = Runner[StartOptions]()

  override def commands: Seq[Command[?]] = List(StartCommand)

  class Runner[T <: AgentServiceAppCommand: {Parser, Help}] extends EswCommand[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      LocationServerStatus.requireUpLocally()
      command match {
        case StartOptions(port) => start(port)
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
}

// $COVERAGE-ON$
