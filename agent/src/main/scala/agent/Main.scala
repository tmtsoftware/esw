package agent

import agent.AgentCliCommand.StartCommand
import agent.AgentCommand.SpawnCommand.SpawnSequenceComponent
import akka.actor.CoordinatedShutdown.{ActorSystemTerminateReason, UnknownReason}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

// todo: Add support for default actions e.g. redis
// todo: merge location-agent
// todo: devmode kills all processes before dying
// todo: try moving this module to csw by merging with location-server
object Main extends CommandApp[AgentCliCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentCliCommand, remainingArgs: RemainingArgs): Unit = command match {
    case StartCommand(machineName) => onStart(machineName)
  }

  private def onStart(machineName: String): Unit = {
    val wiring      = new AgentWiring
    val log: Logger = AgentLogger.getLogger

    try {
      import wiring.scheduler
      wiring.actorRuntime.startLogging(progName, appVersion)

      implicit val timeout: Timeout = Timeout(10.seconds)

      //fixme: fix the hardcoded AgentName
      val agentConnection = AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "Agent"), ComponentType.Machine))
      Await.result(wiring.locationService.register(AkkaRegistration(agentConnection, wiring.agentRef.toURI)), timeout.duration)

      // Test messages
      val response: Future[Response]  = wiring.agentRef ? SpawnSequenceComponent(Prefix(Subsystem.ESW, "primary"))
      val response2: Future[Response] = wiring.agentRef ? SpawnSequenceComponent(Prefix(Subsystem.ESW, "secondary"))
      println("primary Response=" + Await.result(response, 10.seconds))
      println("secondary Response=" + Await.result(response2, 10.seconds))
    }
    catch {
      case NonFatal(ex) =>
        log.error("agent-app crashed", Map("machine-name" -> machineName), ex)
        ex.printStackTrace()
        //shutdown is required so that actor system shuts down gracefully and jvm process can exit
        wiring.actorRuntime.shutdown(UnknownReason)
        exit(1)
    }
  }
}
