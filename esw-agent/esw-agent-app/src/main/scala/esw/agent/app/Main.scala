package esw.agent.app

import java.time.Duration

import AgentCliCommand.StartCommand
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.util.Timeout
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, duration}
import scala.language.implicitConversions
import scala.util.control.NonFatal

object Main extends CommandApp[AgentCliCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentCliCommand, remainingArgs: RemainingArgs): Unit = command match {
    case StartCommand(prefix) => onStart(Prefix(prefix), ConfigFactory.load())
  }

  var wiring: AgentWiring = _

  def onStart(prefix: Prefix, config: Config): Unit = {
    val agentConfig = config.getConfig("agent")
    val agentSettings: AgentSettings = {
      AgentSettings(
        agentConfig.getString("binariesPath"),
        agentConfig.getDuration("durationToWaitForComponentRegistration")
      )
    }
    wiring = new AgentWiring(prefix, agentSettings)
    wiring.log.debug("starting machine agent", Map("prefix" -> prefix))
    try {
      LocationServerStatus.requireUpLocally(5.seconds)
      implicit val timeout: Timeout = Timeout(10.seconds)
      Await.result(wiring.lazyAgentRegistration, timeout.duration)
      wiring.log.info("agent started")
    }
    catch {
      case NonFatal(ex) =>
        wiring.log.error("agent-app crashed", Map("machine-name" -> prefix), ex)
        //shutdown is required so that actor system shuts down gracefully and jvm process can exit
        wiring.actorRuntime.shutdown(UnknownReason)
        exit(1)
    }
  }

  private implicit def asFiniteDuration(d: Duration): FiniteDuration = duration.Duration.fromNanos(d.toNanos)
}
