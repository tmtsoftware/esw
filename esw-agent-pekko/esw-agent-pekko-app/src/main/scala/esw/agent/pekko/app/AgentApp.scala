package esw.agent.pekko.app

import org.apache.pekko.actor.CoordinatedShutdown.{PhaseBeforeServiceUnbind, UnknownReason}
import caseapp.{Command, RemainingArgs}
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.Help
import caseapp.core.parser.Parser
import com.typesafe.config.ConfigFactory
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.agent.pekko.app.AgentCliCommand.StartOptions

import scala.concurrent.Await
import scala.util.control.NonFatal

// $COVERAGE-OFF$
/*
 * The main app to start Agent on a machine.
 * */
object AgentApp extends CommandsEntryPoint {
  def appName: String           = getClass.getSimpleName.dropRight(1) // remove $ from class name
  def appVersion: String        = BuildInfo.version
  override def progName: String = BuildInfo.name

  val StartCommand: Runner[StartOptions] = Runner[StartOptions]()

  override def commands: Seq[Command[_]] = List(StartCommand)

  class Runner[T <: AgentCliCommand: Parser: Help] extends Command[T] {
    override def run(command: T, args: RemainingArgs): Unit = {
      command match {
        case StartOptions(prefix, hostConfigPath, isConfigLocal) =>
          start(AgentSettings(Prefix(prefix), ConfigFactory.load()), hostConfigPath, isConfigLocal)
      }
    }

    private[esw] def start(agentSettings: AgentSettings, hostConfigPath: Option[String], isConfigLocal: Boolean): AgentWiring = {
      val wiring = new AgentWiring(agentSettings, hostConfigPath, isConfigLocal)
      start(wiring)
    }

    private[esw] def start(
                            wiring: AgentWiring,
                            startLogging: Boolean = true
                          ): AgentWiring = {
      import wiring._
      try {
        if (startLogging) actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)
        LocationServerStatus.requireUpLocally()
        log.debug("starting machine agent", Map("prefix" -> prefix))
        Await.result(lazyAgentRegistration, timeout.duration)

        actorRuntime.coordinatedShutdown
          .addTask(PhaseBeforeServiceUnbind, "unregister-agent") { () =>
            log.warn("agent is shutting down. unregistering agent")
            locationService.unregister(agentConnection)
          }

        log.info("agent started")
        wiring
      }
      catch {
        case NonFatal(ex) =>
          ex.printStackTrace()
          log.error("agent-app crashed", Map("machine-name" -> prefix), ex)
          Await.result(actorRuntime.shutdown(UnknownReason), timeout.duration)
          exit(1)
      }
    }
  }

}
// $COVERAGE-ON$
