package esw.agent.app

import akka.actor.CoordinatedShutdown.{PhaseBeforeServiceUnbind, UnknownReason}
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import com.typesafe.config.ConfigFactory
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import esw.agent.app.AgentCliCommand.StartCommand

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

// $COVERAGE-OFF$
object AgentApp extends CommandApp[AgentCliCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentCliCommand, remainingArgs: RemainingArgs): Unit =
    command match {
      case StartCommand(prefix) => start(Prefix(prefix), AgentSettings.from(ConfigFactory.load()))
    }

  private[esw] def start(prefix: Prefix, agentSettings: AgentSettings): Unit = {
    val wiring = new AgentWiring(prefix, agentSettings)
    start(prefix, wiring)
  }

  private[esw] def start(prefix: Prefix, wiring: AgentWiring): Unit = {
    import wiring._
    try {
      actorRuntime.startLogging(BuildInfo.name, BuildInfo.version)
      LocationServerStatus.requireUpLocally(5.seconds)
      log.debug("starting machine agent", Map("prefix" -> prefix))
      Await.result(lazyAgentRegistration, timeout.duration)

      actorRuntime.coordinatedShutdown
        .addTask(PhaseBeforeServiceUnbind, "unregister-agent") { () =>
          log.warn("agent is shutting down. unregistering agent")
          locationService.unregister(agentConnection)
        }

      log.info("agent started")
    }
    catch {
      case NonFatal(ex) =>
        ex.printStackTrace()
        log.error("agent-app crashed", Map("machine-name" -> prefix), ex)
        //shutdown is required so that actor system shuts down gracefully and jvm process can exit
        Await.result(actorRuntime.shutdown(UnknownReason), timeout.duration)
        exit(1)
    }
  }
}
// $COVERAGE-ON$
