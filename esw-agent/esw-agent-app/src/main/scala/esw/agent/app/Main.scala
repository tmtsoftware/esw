package esw.agent.app

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.util.Timeout
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
object Main extends CommandApp[AgentCliCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentCliCommand, remainingArgs: RemainingArgs): Unit = command match {
    case StartCommand(prefix) => onStart(Prefix(prefix), AgentSettings.from(ConfigFactory.load()))
  }

  var wiring: AgentWiring = _

  def onStart(prefix: Prefix, agentSettings: AgentSettings): Unit = {
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
}
// $COVERAGE-ON$
