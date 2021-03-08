package esw.agent.akka.app

import akka.actor.CoordinatedShutdown.{PhaseBeforeServiceUnbind, UnknownReason}
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import com.typesafe.config.ConfigFactory
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.agent.akka.app.AgentCliCommand.StartCommand

import scala.concurrent.Await
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object AgentApp extends CommandApp[AgentCliCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentCliCommand, remainingArgs: RemainingArgs): Unit =
    command match {
      case StartCommand(prefix, hostConfigPath, isConfigLocal) =>
        start(AgentSettings(Prefix(prefix), ConfigFactory.load()), hostConfigPath, isConfigLocal)
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
// $COVERAGE-ON$
