package agent

import agent.AgentCliCommand.StartCommand
import agent.AgentCommand.SpawnCommand.SpawnSequenceComponent
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import caseapp.core.RemainingArgs
import caseapp.core.app.CommandApp
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

// todo: Add support for default actions e.g. redis
// todo: merge location-agent
// todo: devmode kills all processes before dying
// todo: try moving this module to csw by merging with location-server
object Main extends CommandApp[AgentCliCommand] {
  override def appName: String    = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def appVersion: String = BuildInfo.version
  override def progName: String   = BuildInfo.name

  override def run(command: AgentCliCommand, remainingArgs: RemainingArgs): Unit = command match {
    case StartCommand(machineName) => onStart()
  }

  private def onStart(): Unit = {

    val wiring = new AgentWiring
    import wiring._

    wiring.actorRuntime.startLogging(progName, appVersion)

    implicit val timeout: Timeout = Timeout(10.seconds)

    //fixme: fix the hardcoded AgentName
    val agentConnection = AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "Agent"), ComponentType.Machine))
    Await.result(locationService.register(AkkaRegistration(agentConnection, agentRef.toURI)), timeout.duration)

    // Test messages
    val response: Future[Response]  = agentRef ? SpawnSequenceComponent(Prefix(Subsystem.ESW, "primary"))
    val response2: Future[Response] = agentRef ? SpawnSequenceComponent(Prefix(Subsystem.ESW, "secondary"))
    println("primary Response=" + Await.result(response, 10.seconds))
    println("secondary Response=" + Await.result(response2, 10.seconds))
  }
}
